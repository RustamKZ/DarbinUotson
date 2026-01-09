import json
import numpy as np
from algorithms.stationarity_tests import adf_test
from api.cli_commands import CliCommands
from models.requests import DataInput
from models.responses import AdfTestResult

def handle_command(command: str, args: list) -> AdfTestResult:
  handlers = {
    CliCommands.ADF_TEST: _handle_adf_test,
  }
  handler = handlers[command]
  return handler(args)

def _handle_adf_test(args: list[str]) -> AdfTestResult:
  raw_data = json.loads(args[0])
  data_input = DataInput(**raw_data)
  return adf_test(np.array(data_input.values))
