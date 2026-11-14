# Лабораторна робота 4.1 — Security та API Key Filter

## Реалізований варіант

Обов'язкова частина: OAuth2 Resource Server перевіряє JWT Bearer Token.
Варіант 1: власний ApiKeyFilter обробляє заголовок X-API-Key.
Для JWKS використовується WireMock у Docker, як дозволено завданням.

Це навчальний стенд перевірки токенів: WireMock віддає лише публічні ключі,
а локальний інструмент підписує демонстраційні JWT. WireMock не є повноцінним
OAuth Authorization Server і не реалізує login/consent/token endpoint.
Готові токени перевіряються справжнім JwtDecoder із Spring Security.

## Правила доступу

| Запит | Необхідне право JWT |
| --- | --- |
| GET/HEAD `/api/products` та `/api/products/{id}` | `products:read` |
| POST `/api/products` | `products:write` |
| PUT/DELETE `/api/products/{id}` | `products:write` |

Scope у JWT перетворюється Spring Security на authority з префіксом SCOPE_.
Валідний API Key надає обидва права; це один спільний ключ для навчального стенду.
У запиті використовується лише один спосіб аутентифікації.

- Немає облікових даних або вони неправильні: 401, JSON Problem Detail.
- Токен валідний, але потрібного scope немає: 403, JSON Problem Detail.
- Передано одночасно Authorization і X-API-Key: 401, навіть якщо обидва валідні.
- Дублікати заголовка X-API-Key: 401.
- Токени в query string не приймаються.
- Інші маршрути закриті за замовчуванням; сторінки login немає.

JWT повинен мати підпис RS256, довіреного issuer, audience `cosmo-cats-api`,
обов'язковий exp і чинний строк дії. Перевіряється також nbf, якщо він присутній.
Застосовується стандартний допустимий clock skew Spring Security.
API Key порівнюється через MessageDigest.isEqual і не зберігається як credentials
у SecurityContext. Фільтр зареєстрований лише всередині SecurityFilterChain.

Застосунок працює без серверних сесій. CSRF вимкнено для цього API, яке приймає
облікові дані тільки в заголовках; form login та HTTP Basic вимкнено.
Відсутність налаштованого MARKET_API_KEY призводить до помилки запуску,
а не до відкриття анонімного доступу.

## Підготовка локального стенду

Потрібні Docker Desktop, Java 17+ і Python 3 з OpenSSL. Gradle запускається wrapper-ом.
Стан PostgreSQL та інструкції попередньої лабораторної описані в LAB3.md.

1. Якщо `.env` немає, створити його за `.env.example` і задати пароль PostgreSQL.
   Якщо він уже є, не перезаписувати його.
2. Ініціалізувати локальні ключі:

   ```sh
   python3 scripts/local_security.py init
   ```

   Команда створює RSA-ключ і JWKS у `.local-security/` та додає відсутні локальні
   налаштування в `.env`. Значення-заглушки `replace-with-*` для API Key замінюються
   випадковим ключем. Уже створений приватний ключ і справжній API Key зберігаються.
3. Запустити базу та JWKS-сервіс:

   ```sh
   docker compose -p cosmo-cats-lab3 up -d
   ```

   Назва Compose-проєкту залишена від Lab 3, щоб використовувати її наявний volume.
   PostgreSQL доступна на localhost:5433, JWKS — на localhost:8090/.well-known/jwks.json.
   WireMock отримує лише файл jwks.json; приватний ключ до контейнера не монтується.
4. У папці проєкту запустити застосунок:

   ```sh
   set -a
   source .env
   set +a
   DB_URL="jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB}" \
   DB_USERNAME="$POSTGRES_USER" DB_PASSWORD="$POSTGRES_PASSWORD" ./gradlew bootRun
   ```

`.env` та `.local-security/` виключено з Git. У репозиторій потрапляють лише
приклад конфігурації, скрипт і mapping WireMock. Приватні ключі й токени не слід
додавати до PR чи логів. Для іншого OAuth-провайдера налаштувати JWT_ISSUER,
JWT_JWK_SET_URI та JWT_AUDIENCE відповідно до виданих ним токенів.

## Ручна перевірка

У другому терміналі, з папки проєкту:

```sh
set -a
source .env
set +a
```

Без аутентифікації очікується 401:

```sh
curl -i http://localhost:8080/api/products
```

Із ключем очікується 200:

```sh
curl -i -H "X-API-Key: $MARKET_API_KEY" http://localhost:8080/api/products
```

JWT із правами читання та зміни, чинний 15 хвилин:

```sh
TOKEN=$(python3 scripts/local_security.py token)
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/products
```

JWT лише для читання:

```sh
READ_TOKEN=$(python3 scripts/local_security.py token --scope products:read)
curl -i -H "Authorization: Bearer $READ_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Read-only check","price":1,"categoryId":"11111111-1111-1111-1111-111111111111","stock":0}' \
  http://localhost:8080/api/products
```

Останній запит має повернути 403. Не передавати X-API-Key разом із Bearer Token.

## Тести

На поточному Docker Engine 20.10:

```sh
DOCKER_API_VERSION=1.41 ./gradlew clean build
```

Для сучасного Docker і GitHub Actions достатньо `./gradlew clean build`.

Старі MVC-тести виконуються з @WithMockUser і перевіряють попередню поведінку API.
SecurityIntegrationTest виконує реальні перевірки заголовків і підписаних JWT,
отримуючи JWKS з тимчасового локального HTTP-сервера. Приватні ключі цих тестів
генеруються в пам'яті. Є також mock JWT для окремої перевірки правил доступу.
Тести не залежать від локальних секретів або запущеного Compose WireMock.
PostgreSQL для тестів піднімається окремо через Testcontainers.

Звіти: `build/reports/tests/test/index.html` та `build/reports/coverage/index.html`.
Пороги покриття збережено: 80% рядків застосунку, 80% рядків і гілок кожного сервісу.

## Обсяг та Git

Method Level Security та профіль no-auth не реалізовані: це окремі бонусні пункти.
Бізнес-логіку товарів, замовлень, категорій і міграції Lab 3 не змінено.
Гілка роботи: lab4-security від lab3-databases. Коміти, pull, push і PR у межах
цієї реалізації не виконуються. Після майбутньої публікації необхідно окремо
перевірити GitHub Actions; локальна збірка не підтверджує його результат.

## Підтверджені результати — 14.11.2026

- `DOCKER_API_VERSION=1.41 ./gradlew clean build` — успішно.
- 137 тестових випадків: 0 помилок, 0 пропущених.
- Покриття рядків застосунку: 337/362 = 93,09%.
- Новий пакет security: 67/67 рядків, 18/18 гілок = 100%.
- Загальне покриття гілок застосунку: 50/64 = 78,12%; загальний поріг задано
  для рядків. Кожен сервіс зберіг 100% рядків і гілок.
- Compose та OpenAPI успішно розібрано як YAML; для кожної операції Product API
  описано схеми аутентифікації та відповіді 401/403.
- Зібраний jar перевірено через реальний HTTP з PostgreSQL і WireMock у Docker:
  анонімний запит — 401; API Key — 200; підписаний локальний JWT — 200;
  JWT лише для читання при POST — 403; неправильні/змішані дані — 401;
  створення, читання, оновлення та видалення товару з JWT — успішно.
- Тимчасовий товар після перевірки видалено, Java-процес зупинено. PostgreSQL
  та WireMock залишено запущеними для подальшої локальної роботи.
- `.env`, приватний ключ та локальний JWKS виключено з Git. Приватних компонентів
  RSA-ключа у відповіді WireMock немає.
- Комітів, pull, push та нового PR для Lab 4 не було; GitHub CI не перевірено.
