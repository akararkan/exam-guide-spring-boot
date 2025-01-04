package com.ak.exam.user.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Token {
    public String token;
    public String response;
    public Token(String response){
        this.response = response;
    }



}