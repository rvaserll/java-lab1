CREATE SEQUENCE categories_id_seq INCREMENT BY 1;
CREATE SEQUENCE products_id_seq INCREMENT BY 1;
CREATE SEQUENCE orders_id_seq INCREMENT BY 1;
CREATE SEQUENCE order_items_id_seq INCREMENT BY 1;

CREATE TABLE categories (
    id BIGINT PRIMARY KEY DEFAULT nextval('categories_id_seq'),
    public_id UUID NOT NULL UNIQUE,
    name TEXT NOT NULL CHECK (name !~ '^[[:space:]]*$')
);
CREATE TABLE products (
    id BIGINT PRIMARY KEY DEFAULT nextval('products_id_seq'),
    public_id UUID NOT NULL UNIQUE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    product_name VARCHAR(120) NOT NULL CHECK (char_length(product_name) >= 3 AND product_name !~ '^[[:space:]]*$'),
    description VARCHAR(2000),
    price NUMERIC(12,2) NOT NULL CHECK (price > 0),
    stock INTEGER NOT NULL CHECK (stock >= 0)
);
CREATE TABLE orders (
    id BIGINT PRIMARY KEY DEFAULT nextval('orders_id_seq'),
    public_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('CREATED', 'PAID', 'SHIPPED', 'CANCELLED'))
);
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('order_items_id_seq'),
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_name_at_purchase VARCHAR(120) NOT NULL CHECK (char_length(product_name_at_purchase) >= 3 AND product_name_at_purchase !~ '^[[:space:]]*$'),
    unit_price_at_purchase NUMERIC(12,2) NOT NULL CHECK (unit_price_at_purchase > 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0)
);
