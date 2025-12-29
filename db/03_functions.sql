-- 1) Вернуть один (любой) Ticket с минимальным venue (считаем "минимальным" по venue_id).
CREATE OR REPLACE FUNCTION fn_ticket_min_venue()
    RETURNS BIGINT
    LANGUAGE sql
AS $$
SELECT t.id
FROM tickets t
WHERE t.venue_id IS NOT NULL
ORDER BY t.venue_id ASC, t.id ASC
LIMIT 1;
$$;

-- 2) Вернуть массив (set) Ticket id, где venue_id меньше заданного.
CREATE OR REPLACE FUNCTION fn_ticket_ids_with_venue_less_than(p_venue_id BIGINT)
    RETURNS TABLE(ticket_id BIGINT)
    LANGUAGE sql
AS $$
SELECT t.id
FROM tickets t
WHERE t.venue_id IS NOT NULL
  AND t.venue_id < p_venue_id
ORDER BY t.id ASC;
$$;

-- 3) Вернуть массив уникальных значений number по всем Ticket.
CREATE OR REPLACE FUNCTION fn_ticket_unique_numbers()
    RETURNS TABLE(num BIGINT)
    LANGUAGE sql
AS $$
SELECT DISTINCT t.number
FROM tickets t
ORDER BY t.number ASC;
$$;

-- 4) Создать новый Ticket на основе указанного:
--    discount = заданный процент
--    price увеличиваем на ту же сумму, что составляет скидка от исходной цены:
--      delta = old_price * percent / 100
--      new_price = old_price + delta
CREATE OR REPLACE FUNCTION fn_ticket_clone_with_discount_raise(
    p_ticket_id BIGINT,
    p_discount_percent BIGINT
)
    RETURNS BIGINT
    LANGUAGE plpgsql
AS $$
DECLARE
    src tickets%ROWTYPE;
    delta DOUBLE PRECISION;
    new_id BIGINT;
BEGIN
    IF p_discount_percent IS NULL OR p_discount_percent < 1 OR p_discount_percent > 100 THEN
        RAISE EXCEPTION 'discount percent must be in [1..100], got %', p_discount_percent;
    END IF;

    SELECT *
    INTO src
    FROM tickets
    WHERE id = p_ticket_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'ticket not found: %', p_ticket_id;
    END IF;

    delta := src.price * (p_discount_percent::DOUBLE PRECISION) / 100.0;

    INSERT INTO tickets(
        name,
        coordinates_x, coordinates_y,
        creation_date,
        person_id, event_id, venue_id,
        price, type, discount, number, comment, refundable
    )
    VALUES (
               src.name,
               src.coordinates_x, src.coordinates_y,
               CURRENT_DATE,
               src.person_id, src.event_id, src.venue_id,
               src.price + delta, src.type, p_discount_percent, src.number, src.comment, src.refundable
           )
    RETURNING id INTO new_id;

    RETURN new_id;
END;
$$;

-- 5) Отменить все бронирования указанного человека.
CREATE OR REPLACE FUNCTION fn_cancel_person_bookings(p_person_id BIGINT)
    RETURNS BIGINT
    LANGUAGE plpgsql
AS $$
DECLARE
    affected BIGINT;
BEGIN
    UPDATE tickets
    SET person_id = NULL
    WHERE person_id = p_person_id;

    GET DIAGNOSTICS affected = ROW_COUNT;
    RETURN affected;
END;
$$;
