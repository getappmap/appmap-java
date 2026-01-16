SELECT * FROM customer WHERE id = ? -- op 1
SELECT * FROM customer WHERE id = ? -- op 1
SELECT first_name FROM customer WHERE id = ? -- op 2
SELECT first_name FROM customer WHERE id = ? -- op 2
UPDATE customer SET last_name = ? WHERE id = ? -- op 3
UPDATE customer SET last_name = ? WHERE id = ? -- op 3
UPDATE customer SET first_name = ? WHERE id = ? -- op 4
UPDATE customer SET first_name = ? WHERE id = ? -- op 4
UPDATE customer SET last_name = ? WHERE id = ? -- op 5
UPDATE customer SET last_name = ? WHERE id = ? -- op 5
SELECT count(*) FROM customer WHERE id = ? -- op 6
SELECT count(*) FROM customer WHERE id = ? -- op 6
