ALTER TABLE products ADD CONSTRAINT uk_products_category_name UNIQUE (category_id, product_name);
