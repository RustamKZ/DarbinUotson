import sys
import json
from dataclasses import asdict
from api.cli import handle_command
from models.responses import ErrorResponse

def main():
  if len(sys.argv) < 2:
    error = ErrorResponse(
      error="NO_COMMAND",
      message="No command specified"
    )
    print(json.dumps(asdict(error)))
    sys.exit(1)
  
  command = sys.argv[1]
  args = sys.argv[2:] if len(sys.argv) > 2 else []
  
  try:
    result = handle_command(command, args)
    print(json.dumps(asdict(result), default=str))
  except Exception as e:
    error = ErrorResponse(
      error="EXECUTION_ERROR",
      message=str(e)
    )
    print(json.dumps(asdict(error), default=str))
    sys.exit(1)

if __name__ == "__main__":
  main()
