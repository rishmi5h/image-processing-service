package com.imageprocessing.model;


import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class AuthInfoResponse {
    public String username;
    public String password;
    public String jwt;
}
