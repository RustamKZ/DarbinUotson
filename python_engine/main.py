import sys
import json
from api.analyzer import analyze_time_series
from models.responses import ErrorResponse
from dataclasses import asdict

def main():
  try:
    input_json = sys.stdin.read().strip()

    if not input_json:
      error = ErrorResponse(
        error="NO_INPUT",
        message="No input data provided"
      )
      print(json.dumps(asdict(error)))
      sys.exit(1)

    result = analyze_time_series(input_json)
    print(result)

  except Exception as e:
    error = ErrorResponse(
      error="EXECUTION_ERROR",
      message=str(e)
    )
    print(json.dumps(asdict(error)))
    sys.exit(1)

if __name__ == "__main__":
  main()