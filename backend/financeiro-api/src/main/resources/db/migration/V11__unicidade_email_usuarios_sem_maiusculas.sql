UPDATE usuarios SET email = LOWER(TRIM(email)) WHERE email <> LOWER(TRIM(email));

CREATE UNIQUE INDEX uk_usuarios_email_minusculo ON usuarios (LOWER(email));
