import logging
import requests
import pandas as pd
from sentence_transformers import SentenceTransformer, util
import os
import json

# Configure professional logging
logging.basicConfig(level=logging.INFO, format='[%(levelname)s] %(message)s')

# ==========================================
# BUSINESS LOGIC SCORERS
# ==========================================
def location_score(user_nuts, bid_nuts):
    """
    Advanced hierarchical scoring for NUTS geographical codes.
    Optimized for the Greek NUTS structure (EL000).
    """
    # 1. Robust Null & Type Checking
    if not user_nuts or not bid_nuts or pd.isna(user_nuts) or pd.isna(bid_nuts):
        return 0.0
    
    # 2. Sanitization (Remove whitespace and force uppercase)
    u = str(user_nuts).strip().upper()
    b = str(bid_nuts).strip().upper()
    
    # --- SCORING HIERARCHY ---

    # A. Exact Match (e.g., EL303 == EL303) -> Perfect
    if u == b:
        return 1.0
        
    # B. Country-Wide Tenders (If bid is just "EL" and matches user's country)
    # High priority because national tenders are usually high-value.
    if len(b) == 2 and b == u[:2]:
        return 0.85
        
    # C. Same Prefecture/Sub-region (NUTS 2 Match - e.g., EL30 vs EL301)
    # Both must have at least 4 chars to be considered NUTS 2 level.
    if len(u) >= 4 and len(b) >= 4 and u[:4] == b[:4]:
        return 0.75
        
    # D. Same Broad Region (NUTS 1 Match - e.g., EL5 vs EL52)
    # Matches the general area (e.g., Northern Greece).
    if len(u) >= 3 and len(b) >= 3 and u[:3] == b[:3]:
        return 0.50
        
    # E. Same Country (NUTS 0 Match - e.g., EL3 vs EL6)
    # Still in Greece, but in a different region.
    if u[:2] == b[:2]:
        return 0.20
        
    return 0.0

def cpv_score(user_cpv, bid_cpv):
    """Evaluates industry relevance using hierarchical CPV code prefix matching."""
    if not user_cpv or not bid_cpv:
        return 0.0
    
    # Cast to string and strip whitespace to prevent slicing errors
    user_cpv, bid_cpv = str(user_cpv).strip(), str(bid_cpv).strip()
    
    if len(bid_cpv) >= 8 and bid_cpv[:8] == user_cpv[:8]:
        return 1.0
    if len(bid_cpv) >= 5 and bid_cpv[:5] == user_cpv[:5]:
        return 0.75
    if len(bid_cpv) >= 3 and bid_cpv[:3] == user_cpv[:3]:
        return 0.5
    if len(bid_cpv) >= 2 and bid_cpv[:2] == user_cpv[:2]:
        return 0.25
    return 0.0


def main():
    BASE_API_URL = "http://localhost:8080/api"
    OUTPUT_DIR = "recommendations"
    
    try:
        logging.info("AI ENGINE: Synchronizing with Spring Boot Backend...")
        bids_data = requests.get(f"{BASE_API_URL}/search-bid/all").json()
        users_data = requests.get(f"{BASE_API_URL}/users").json()
        
        if not bids_data or not users_data:
            logging.warning("AI ENGINE: Insufficient data found. Check Ingestion logs.")
            return

        df_bids = pd.DataFrame(bids_data)
        model = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')
        
        # Semantic context generation
        df_bids['text_content'] = df_bids['title'].fillna("") + " " + df_bids.get('description', "").fillna("")
        bid_embeddings = model.encode(df_bids['text_content'].tolist(), convert_to_tensor=True)

        os.makedirs(OUTPUT_DIR, exist_ok=True)

        for i, u_data in enumerate(users_data, start=1):
            user_name = u_data.get("fullName", "Unknown")
            logging.info(f"AI ENGINE: Scoring for User: {user_name}")

            # 1. Budget Constraints (Strict Filter)
            c_min = float(u_data.get("preferredCostMin") or 0.0)
            c_max = float(u_data.get("preferredCostMax") or 1e12)
            
            # 2. Semantic Query from Interests
            interests = [itst["name"] for itst in u_data.get("interests", [])]
            user_query = ", ".join(interests)
            if not user_query.strip(): continue

            user_embedding = model.encode(user_query, convert_to_tensor=True)
            semantic_scores = util.cos_sim(user_embedding, bid_embeddings)[0].cpu().numpy()
            df_bids["semantic_score"] = semantic_scores

            # Filter candidates based on HARD budget constraints
            user_bids = df_bids[(df_bids["cost"] >= c_min) & (df_bids["cost"] <= c_max)].copy()

            if user_bids.empty:
                logging.info(f"AI ENGINE: No matches found in budget range for {user_name}")
                continue

            # 3. Hybrid Scoring Calculation
            user_bids["loc_score"] = user_bids["nutsCode"].apply(lambda x: location_score(u_data.get("nutsCode"), x))
            user_bids["cpv_score"] = user_bids["cpvCode"].apply(lambda x: cpv_score(u_data.get("cpvCode"), x))

            # Hybrid Formula: 30% Semantic, 40% CPV (Industry), 30% Location
            user_bids["final_score"] = (0.3 * user_bids["semantic_score"]) + (0.4 * user_bids["cpv_score"]) + (0.3 * user_bids["loc_score"])

            recommendations = user_bids.sort_values(by="final_score", ascending=False).head(10)

            # Exporting localized JSON results
            output = []
            for _, row in recommendations.iterrows():
                output.append({
                    "title": row["title"],
                    "budget": f"€{row['cost']:,.2f}",
                    "cpv": row["cpvCode"],
                    "location": row["nutsCode"],
                    "scores": {
                        "semantic": f"{row['semantic_score']:.2%}",
                        "industry": f"{row['cpv_score']:.2%}",
                        "region": f"{row['loc_score']:.2%}",
                        "total": f"{row['final_score']:.2%}"
                    }
                })

            with open(f"{OUTPUT_DIR}/{user_name.replace(' ', '_')}.json", "w", encoding="utf-8") as f:
                json.dump(output, f, ensure_ascii=False, indent=2)

        logging.info("AI ENGINE: All recommendation files generated successfully.")

    except Exception as e:
        logging.error(f"AI ENGINE FAILURE: {e}")

if __name__ == "__main__":
    main()