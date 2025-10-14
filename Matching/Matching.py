import logging
import requests
import pandas as pd
from sqlalchemy import create_engine
from sentence_transformers import SentenceTransformer, util
import torch
import os
import json

# Setup logging
logging.basicConfig(level=logging.INFO, format='[%(levelname)s] %(message)s')

# Database connection
engine = create_engine("mysql+mysqlconnector://root:password@localhost/bid_incoming")
logging.info("Connected to the database.")

# Fetch users from backend
USER_API_URL = "http://localhost:8080/users"
response = requests.get(USER_API_URL)
users_data = response.json()
logging.info(f"Fetched {len(users_data)} users from backend.")

# Define user class
class User:
    def __init__(self, fullName, age, sex, interests, preferredCostMin, preferredCostMax, preferredOrganizations, userNutsCode, userCPVCode):
        self.fullName = fullName
        self.age = age
        self.sex = sex
        self.interests = [i["name"] for i in interests if isinstance(i, dict) and "name" in i]
        self.preferredCostMin = preferredCostMin
        self.preferredCostMax = preferredCostMax
        self.preferredOrganizations = preferredOrganizations
        self.userNutsCode = userNutsCode
        self.userCPVCode = userCPVCode

# Load bid data
query = "SELECT title, description, organization, cost, nuts_code, cpv_code, date FROM bid"
df = pd.read_sql(query, engine)
logging.info(f"Loaded {len(df)} bids from the database.")

# Load model once
logging.info("Loading sentence transformer model...")
model = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')

# Scoring functions
def location_score(user_nuts, bid_nuts):
    if not bid_nuts or not user_nuts:
        return 0.0
    if bid_nuts == user_nuts:
        return 1.0
    if bid_nuts[:4] == user_nuts[:4]:
        return 0.75
    if bid_nuts[:3] == user_nuts[:3]:
        return 0.5
    if bid_nuts[:2] == user_nuts[:2]:
        return 0.25
    return 0.0

def cpv_score(user_cpv, bid_cpv):
    if not user_cpv or not bid_cpv:
        return 0.0
    if bid_cpv[:8] == user_cpv[:8]:
        return 1.0
    if bid_cpv[:5] == user_cpv[:5]:
        return 0.75
    if bid_cpv[:3] == user_cpv[:3]:
        return 0.5
    if bid_cpv[:2] == user_cpv[:2]:
        return 0.25
    return 0.0

# Create output directory
os.makedirs("recommendations", exist_ok=True)

# Run recommendations for each user
for i, user_data in enumerate(users_data, start=1):
    logging.info(f"Processing user {i}/{len(users_data)}: {user_data['fullName']}")
    logging.info(f"User data: {json.dumps(user_data, ensure_ascii=False, indent=2)}")

    user = User(
        fullName=user_data["fullName"],
        age=user_data["age"],
        sex=user_data["sex"],
        interests=user_data.get("interests", []),
        preferredCostMin=user_data.get("preferredCostMin", 0),
        preferredCostMax=user_data.get("preferredCostMax", float("inf")),
        preferredOrganizations=user_data.get("preferredOrganizations", []),
        userNutsCode=user_data.get("nutsCode", ""),
        userCPVCode=user_data.get("cpvCode", "")
    )

    logging.info(f"User interests: {user.interests}")

    user_text = ", ".join(user.interests)
    if not user_text.strip():
        logging.warning(f"Skipping user {user.fullName} due to empty interests.")
        continue

    user_embedding = model.encode(user_text, convert_to_tensor=True)

    texts = df.apply(lambda row: f"{row['title']} {row.get('description', '')} {row['organization']}", axis=1).tolist()
    if not texts:
        logging.warning("No bid texts available for embedding.")
        continue

    bid_embeddings = model.encode(texts, convert_to_tensor=True, batch_size=32, show_progress_bar=False)
    if bid_embeddings.shape[0] == 0:
        logging.warning("No bid embeddings generated.")
        continue

    semantic_scores = util.cos_sim(user_embedding, bid_embeddings)[0].cpu().numpy()
    df["semantic_score"] = semantic_scores

    df["location_score"] = df["nuts_code"].apply(lambda nuts: location_score(user.userNutsCode, nuts))
    df["cpv_score"] = df["cpv_code"].apply(lambda bid_cpv: cpv_score(user.userCPVCode, bid_cpv))

    df["combined_score"] = (
        0.3 * df["semantic_score"] +
        0.4 * df["cpv_score"] +
        0.3 * df["location_score"]
    )

    df_filtered = df[
        (df["cost"] >= user.preferredCostMin) &
        (df["cost"] <= user.preferredCostMax)
    ].drop_duplicates(subset=["title", "organization"])

    top_bids = df_filtered.sort_values(by="combined_score", ascending=False).head(10)

    recommendations = []
    for row in top_bids.itertuples():
        reasons = []
        if row.location_score >= 1:
            reasons.append("είναι στην περιοχή σας")
        elif row.location_score >= 0.50:
            reasons.append("είναι σε κοντινή γεωγραφική περιοχή")
        if row.semantic_score >= 0.75:
            reasons.append("ταιριάζει με τα ενδιαφέροντά σας")
        if row.cpv_score >= 0.70:
            reasons.append("αντιστοιχεί σε σχετικό CPV κωδικό")

        summary = " και ".join(reasons) if reasons else "έχει σχετική συνάφεια"

        recommendations.append({
            "title": row.title,
            "organization": row.organization,
            "cost": row.cost,
            "cpv_code": row.cpv_code,
            "nuts_code": row.nuts_code,
            "date": str(row.date),
            "location_score": round(row.location_score, 2),
            "semantic_score": round(row.semantic_score, 2),
            "cpv_score": round(row.cpv_score, 2),
            "combined_score": round(row.combined_score, 2),
            "reason": f"Η προσφορά {summary}."
        })

    safe_name = user.fullName.replace(" ", "_")
    output_path = os.path.join("recommendations", f"{i}_{safe_name}_recommendations.json")
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(recommendations, f, ensure_ascii=False, indent=2)

    logging.info(f"Saved recommendations for {user.fullName} to {output_path}")
