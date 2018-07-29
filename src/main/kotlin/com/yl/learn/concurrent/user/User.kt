package com.yl.learn.concurrent.user

import java.io.Serializable

public class User : Serializable {

    constructor(name : String, sex : String) {
        this.name = name;
        this.sex = sex;
    }

    var name : String = "用户";

    var sex : String = "男";

}