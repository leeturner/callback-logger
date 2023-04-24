DROP TABLE IF EXISTS callback;

CREATE TABLE callback
(
    id           BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    timestamp    TIMESTAMP NOT NULL,
    http_method  VARCHAR(10),
    http_version VARCHAR(10),
    payload      CLOB      NOT NULL
);