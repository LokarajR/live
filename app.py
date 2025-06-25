from flask import Flask, request, jsonify
import google.generativeai as genai
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

genai.configure(api_key="AIzaSyDCaFFGV_5f_wu-deQkqgGfqCvLOcoohPc")

@app.route("/api/gemini", methods=["POST"])
def ask_gemini():
    data = request.get_json()
    prompt = data.get("message", "")
    try:
        model = genai.GenerativeModel("gemini-pro")
        response = model.generate_content(prompt)
        return jsonify({"response": response.text})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run()
