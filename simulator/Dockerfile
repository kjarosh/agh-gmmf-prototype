FROM python:3.7

# requirements
COPY requirements.txt .
RUN pip install -r requirements.txt && rm requirements.txt

COPY src/python .
CMD ["python", "-m", "main"]
