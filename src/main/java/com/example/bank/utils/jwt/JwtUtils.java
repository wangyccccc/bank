package com.example.bank.utils.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.bank.utils.jwt.enums.AuthData;

import java.util.Date;

public class JwtUtils {

    /**
     * authorization 开头前缀
     */
    public static final String AUTHORIZATION_PREFIX = "Bearer ";

    /**
     * 生成 authorization
     *
     * @param id        ID
     * @param expiresAt 过期时间
     * @param secret    密钥
     * @return authorization
     */
    public static String create(Long id, Date expiresAt, String secret) {
        return JWT.create()
                .withClaim(AuthData.ID.toString(), id.toString())
                .withIssuedAt(new Date())
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 校验过期时间（过期将会报错）
     *
     * @param authorization authorization
     * @return 校验过期时间（过期将会报错）
     */
    public static boolean verify(String authorization, String secret) {
        authorization = authorization.replace(JwtUtils.AUTHORIZATION_PREFIX, "");
        try {
            JWT.require(Algorithm.HMAC256(secret)).build().verify(authorization);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 直接获取载体（过期不会报错）
     *
     * @param authorization token
     * @return 载体
     */
    private static DecodedJWT getTokenBody(String authorization) {
        try {
            return JWT.decode(authorization);
        } catch (Exception e) {
            throw new JWTDecodeException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * 获取ID
     *
     * @param authorization authorization
     * @return 获取ID
     */
    public static Long getId(String authorization) {
        authorization = authorization.replace(JwtUtils.AUTHORIZATION_PREFIX, "");
        Claim claim = getTokenBody(authorization).getClaim(AuthData.ID.toString());
        if (claim.isNull()) {
            throw new JWTDecodeException("访问凭证已失效，请重新登录：缺少 ID");
        }
        return Long.parseLong(claim.asString());
    }

}