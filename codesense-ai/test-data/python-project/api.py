"""
Flask API routes for the sample Python project.
"""

from flask import Flask, jsonify, request

app = Flask(__name__)

users_db = {}


@app.route('/api/users', methods=['GET'])
def get_users():
    """Return all users."""
    return jsonify(list(users_db.values()))


@app.route('/api/users/<int:user_id>', methods=['GET'])
def get_user(user_id):
    """Return a specific user by ID."""
    user = users_db.get(user_id)
    if not user:
        return jsonify({"error": "User not found"}), 404
    return jsonify(user)


@app.route('/api/users', methods=['POST'])
def create_user():
    """Create a new user."""
    data = request.get_json()
    if not data or not data.get('name') or not data.get('email'):
        return jsonify({"error": "Name and email are required"}), 400
    user_id = len(users_db) + 1
    users_db[user_id] = {"id": user_id, "name": data['name'], "email": data['email']}
    return jsonify(users_db[user_id]), 201


if __name__ == '__main__':
    app.run(debug=False, port=5000)
