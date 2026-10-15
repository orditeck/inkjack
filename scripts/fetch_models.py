#!/usr/bin/env python3
"""Fetch pinned, SHA-256 verified Apache-2.0 OCR data at build time, never at runtime."""
import hashlib
import pathlib
import sys
import urllib.request

MODELS = {
    "eng": "7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2",
    "fra": "ced037562e8c80c13122dece28dd477d399af80911a28791a66a63ac1e3445ca",
}
BASE = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/4.1.0"


def prepare(target):
    target.mkdir(parents=True, exist_ok=True)
    for language, digest in MODELS.items():
        destination = target / f"{language}.traineddata"
        if destination.is_file() and hashlib.sha256(destination.read_bytes()).hexdigest() == digest:
            continue
        with urllib.request.urlopen(f"{BASE}/{destination.name}", timeout=90) as response:
            data = response.read(16 * 1024 * 1024)
        if hashlib.sha256(data).hexdigest() != digest:
            raise RuntimeError(f"Checksum mismatch for {language}; refusing to package unverified data")
        temporary = destination.with_suffix(".tmp")
        temporary.write_bytes(data)
        temporary.replace(destination)
        print(f"Verified {destination.name}")


if __name__ == "__main__":
    prepare(pathlib.Path(sys.argv[1]))
