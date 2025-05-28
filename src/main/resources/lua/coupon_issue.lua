-- KEYS[1] : coupon key (e.g., coupon:1)
-- KEYS[2] : user key (e.g., coupon:user:1)
-- ARGV[1] : current timestamp
-- ARGV[2] : user ID

-- Check if user already has this coupon
local hasCoupon = redis.call('SISMEMBER', KEYS[2], ARGV[2])
if hasCoupon == 1 then
    return 0 -- Already issued
end

-- Get current stock
local stock = tonumber(redis.call('GET', KEYS[1]))
if not stock or stock <= 0 then
    return -1 -- Out of stock
end

-- Decrement stock
redis.call('DECR', KEYS[1])

-- Add coupon to user's set
redis.call('SADD', KEYS[2], ARGV[2])

-- Set expiration for user's coupon set (30 days)
redis.call('EXPIRE', KEYS[2], 30 * 24 * 60 * 60)

return 1 -- Success
