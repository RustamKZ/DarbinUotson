import sys
import json
from api.analyzer import analyze_time_series
from models.responses import ErrorResponse
from dataclasses import asdict

def main():
  #input format: {"y": [1,2,3], "x": [4,5,6]}
  if len(sys.argv) < 2:
    error = ErrorResponse(
      error="NO_INPUT",
      message="No input data provided"
    )
    print(json.dumps(asdict(error)))
    sys.exit(1)

  try:
    input_json = sys.argv[1]
    result = analyze_time_series(input_json)
    print(result)  #JSON string

  except Exception as e:
    error = ErrorResponse(
      error="EXECUTION_ERROR",
      message=str(e)
    )
    print(json.dumps(asdict(error)))
    sys.exit(1)


if __name__ == "__main__":
  main()
