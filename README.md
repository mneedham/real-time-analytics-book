# Real-Time Analytics Book Code Repository

The code for Dunith Dhanushka and Mark Needham's upcoming book on Real-Time Analytics.

*Put the architecture diagram here*

## Install components

```bash
docker-compose up
```

## (Optional) Downloading products

Setup Python environment:

```bash
python -m venv venv
source venv/bin/activate
```

Install dependencies:

```bash
pip install -r scripts/requirements.txt
```

Downloading product pages:

```bash
python scripts/download_products.py
```

Scrape product data:

```bash
python scripts/scrape_products.py
```

The products will be written to [simulator/data/products.json](simulator/data/products.json)