package com.redis;

import redis.clients.jedis.Jedis;

public class conn {
    public static void main(String[] args) {
        Jedis redis = new Jedis("localhost",6379);
        System.out.println();
    }
}
