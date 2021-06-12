import numpy as np
import sys
import os
import os.path
import logging

logging.basicConfig(level=logging.INFO, format='%(levelname)s [ %(name)s @ %(asctime)s ]: %(message)s', stream=sys.stderr)
logger = logging.getLogger("calculate-average-report")

if len(sys.argv) < 1:
    logger.critical("Not enough arguments")
    os._exit(1)

source_dir = sys.argv[1]

def extract_value(filename):
    has_value = lambda x: 'Operations per second' in x
    logger.debug(f"File {filename} is open")
    with open(filename, 'r') as file:
        lines = file.readlines()
    
    matching = list(filter(has_value, lines))
    logger.debug(matching)
    return float(matching[0].split(':')[-1].strip())

def get_report_file(dir):
    return f"{source_dir}/{dir}/report.txt"

files = list(map(get_report_file,
    filter(
    lambda x: os.path.isdir(f"{source_dir}/{x}") and os.path.exists(get_report_file(x)),
    os.listdir(source_dir))
))

if len(files) == 0:
    logger.critical("0 input files found. Terminating")
    sys._exit(1)
else:
    logger.info("Found {} report files: {}".format(len(files), files))

values = map(extract_value, files)
values = np.array(list(values))

logger.debug(values)

avg = np.mean(values)
std = np.std(values)

if np.isnan(avg) or np.isnan(std):
    logger.critical("Eiter average or std is NaN")
    os._exit(1)

print(f"{avg},{std}")

logger.info("Calculate-average-report completed")
