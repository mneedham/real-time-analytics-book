FROM python:3.9.9-bullseye

WORKDIR /workdir

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN mkdir -p data
COPY data/products.json data

ENTRYPOINT ["python", "seeder.py"]