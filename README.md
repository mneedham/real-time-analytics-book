# Real-Time Analytics Book Code Repository

The code for Dunith Dhanushka and Mark Needham's upcoming book on Real-Time Analytics.

## Downloading products

Setup Python environment:

```
python -m venv venv
source venv/bin/activate
```

Install dependencies:

```
pip install -r scripts/requirements.txt
```

Downloading product pages:

```
python scripts/download_products.py
```

Scrape product data:

```
python scripts/scrape_products.py
```

The products will be written to [simulator/data/products.json](simulator/data/products.json)