"""
ML model wrapper for the mixed-language test project.
"""

import json
from typing import List, Dict


class TextClassifier:
    """Simple text classification model."""

    def __init__(self, model_path: str):
        self.model_path = model_path
        self.labels = ['positive', 'negative', 'neutral']
        self._model = None

    def load(self) -> None:
        """Load the model from disk."""
        # Placeholder for actual model loading
        self._model = {"loaded": True, "path": self.model_path}

    def predict(self, texts: List[str]) -> List[Dict]:
        """Predict labels for input texts."""
        if not self._model:
            raise RuntimeError("Model not loaded. Call load() first.")
        return [{"text": t, "label": self.labels[0], "confidence": 0.85} for t in texts]

    def preprocess(self, text: str) -> str:
        """Preprocess input text."""
        return text.strip().lower()
