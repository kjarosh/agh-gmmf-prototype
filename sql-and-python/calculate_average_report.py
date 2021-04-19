import numpy as np
import sys
import os
import os.path
import logging

logging.basicConfig(level=logging.INFO, format='%(levelname)s [ %(name)s @ %(asctime)s ]: %(message)s')
logger = logging.getLogger("calculate-average-report")

output_file = sys.argv[2]
source_dir = sys.argv[1]

def extract_value(filename):
    has_value = lambda x: 'Operations per second' in x
    logger.debug(f"File {filename} is open")
    with open(filename, 'r') as file:
        lines = file.readlines()
        logger.debug(lines)
    return float(list(filter(has_value, lines))[0].split(':')[1].strip())

def get_report_file(dir):
    return f"{source_dir}/{dir}/raport.txt"

if len(sys.argv) < 3:
    logger.critical("Not enough arguments")
    os._exit(1)


files = list(map(get_report_file,
    filter(
    lambda x: os.path.isdir(f"{source_dir}/{x}") and os.path.exists(get_report_file(x)),
    os.listdir(source_dir))
))

logger.info("Found {} report files: {}".format(len(files), files))

values = map(extract_value, files)
values = np.array(list(values))

logger.debug(values)

avg = np.mean(values)
std = np.std(values)

if np.isnan(avg) or np.isnan(std):
    logger.critical("Eiter average or std is NaN")
    os._exit(1)


with open(output_file, 'a') as file:
    if os.path.exists(output_file) and os.path.getsize(output_file) > 0:
        logger.warning(f"File {output_file} exists. Appending to existing file.")
    else:
        file.write("name,avg,std\n")
    file.write(f"{source_dir},{avg},{std}\n")
    logger.debug(f"Results written to file {output_file}")

logger.info("Calculate-average-report completed")