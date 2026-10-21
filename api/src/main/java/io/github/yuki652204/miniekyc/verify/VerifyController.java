package io.github.yuki652204.miniekyc.verify;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VerifyController {

    private final VerifyService service;

    public VerifyController(VerifyService service) {
        this.service = service;
    }

    /** 検証に失敗しても 200 で {"verified":false,"reason":"..."} を返す */
    @PostMapping("/verify")
    public VerifyResponse verify(@RequestBody VerifyRequest request) {
        return service.verify(request);
    }
}
