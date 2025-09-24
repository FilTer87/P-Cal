package com.privatecal.service;

import com.privatecal.dto.TwoFactorSetupResponse;
import com.privatecal.entity.User;
import com.privatecal.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

@Service
public class TwoFactorService {

    private static final Logger logger = LoggerFactory.getLogger(TwoFactorService.class);
    private static final String APP_NAME = "P-Cal";

    @Autowired
    private UserRepository userRepository;

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier verifier;

    public TwoFactorService() {
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        this.verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    public TwoFactorSetupResponse setupTwoFactor(User user) {
        String secret = secretGenerator.generate();

        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(APP_NAME)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrCodeUrl;
        try {
            byte[] imageData = qrGenerator.generate(data);
            qrCodeUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageData);
        } catch (QrGenerationException e) {
            logger.error("Failed to generate QR code for user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to generate QR code", e);
        }

        String manualEntryKey = formatSecretForManualEntry(secret);

        return new TwoFactorSetupResponse(secret, qrCodeUrl, manualEntryKey);
    }

    public boolean verifyCode(String secret, String code) {
        return verifier.isValidCode(secret, code);
    }

    @Transactional
    public void enableTwoFactor(User user, String secret) {
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        logger.info("2FA enabled for user: {}", user.getUsername());
    }

    @Transactional
    public void disableTwoFactor(User user) {
        user.setTwoFactorSecret(null);
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        logger.info("2FA disabled for user: {}", user.getUsername());
    }

    private String formatSecretForManualEntry(String secret) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < secret.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(secret.charAt(i));
        }
        return formatted.toString();
    }
}