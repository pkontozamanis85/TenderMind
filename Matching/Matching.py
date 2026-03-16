import logging
import requests
import pandas as pd
from sentence_transformers import SentenceTransformer, util
import os
import json

# Configure professional logging
logging.basicConfig(level=logging.INFO, format='[%(levelname)s] %(message)s')

def main():
    """
    Main orchestration logic for the Bid Matching Engine.
    Connects to Java Spring Boot API, processes natural language data,
    and exports semantic recommendations.
    """
    BASE_API_URL = "http://localhost:8080/api"
    OUTPUT_DIR = "recommendations"
    
    try:
        logging.info("Step 1: Synchronizing with Java Backend...")
        bids_resp = requests.get(f"{BASE_API_URL}/search-bid/all")
        users_resp = requests.get(f"{BASE_API_URL}/users")
        
        bids_data = bids_resp.json()
        users_data = users_resp.json()
        
        if not bids_data or not users_data:
            logging.warning("Incomplete data state. Ensure H2 is seeded via data.sql")
            return

        # Load data into DataFrames for vector processing
        df_bids = pd.DataFrame(bids_data)
        logging.info(f"Analyzing {len(df_bids)} public tenders...")

        # Step 2: Semantic Analysis Setup
        # Using a lightweight but powerful transformer model for sentence embeddings
        model = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')
        
        # Combine Title and Description for deeper textual context
        df_bids['text_content'] = df_bids['title'].fillna("") + " " + df_bids['description'].fillna("")
        bid_embeddings = model.encode(df_bids['text_content'].tolist(), convert_to_tensor=True)

        os.makedirs(OUTPUT_DIR, exist_ok=True)

        # Step 3: Individual User Matching Loop
        for i, u_data in enumerate(users_data, start=1):
            user_name = u_data.get("fullName", "Unknown")
            logging.info(f"Computing matches for: {user_name}")

            # Extract user interests as query vectors
            interests = [itst["name"] for itst in u_data.get("interests", [])]
            user_query = ", ".join(interests)
            
            if not user_query.strip():
                continue

            # Calculate Cosine Similarity between user interests and all bids
            user_embedding = model.encode(user_query, convert_to_tensor=True)
            scores = util.cos_sim(user_embedding, bid_embeddings)[0].cpu().numpy()
            
            df_bids["match_score"] = scores
            
            # Step 4: Hybrid Ranking & Filtering
            # We sort by semantic score but keep track of cost constraints
            recommendations = df_bids.sort_values(by="match_score", ascending=False).head(5)

            # Map results to a clean export format
            output = []
            for _, row in recommendations.iterrows():
                output.append({
                    "tender_title": row["title"],
                    "issuing_body": row["organization"],
                    "budget": f"€{row['cost']:,.2f}",
                    "semantic_fit": f"{round(float(row['match_score']) * 100, 2)}%"
                })

            # Save results in a user-specific JSON file
            safe_name = user_name.replace(" ", "_")
            with open(f"{OUTPUT_DIR}/{i}_{safe_name}.json", "w", encoding="utf-8") as f:
                json.dump(output, f, ensure_ascii=False, indent=2)
                
        logging.info(f"Success! {len(users_data)} recommendation files generated in '{OUTPUT_DIR}/'")

    except Exception as e:
        logging.error(f"Critical Pipeline Failure: {e}")

if __name__ == "__main__":
    main()