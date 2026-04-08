package com.copanexus.api.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Value("${RESEND_API_KEY}")
    private String apiKey;

    @Async
    public void enviarEmailBoasVindas(String para, String nomeUsuario) {
        Resend resend = new Resend(apiKey);

        String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #F1F5F9; border-radius: 10px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'>"
                + "<h1 style='color: #102C57; font-size: 28px; margin: 0; font-weight: 900; letter-spacing: -1px;'>COPA<span style='color: #FE0000;'>NEXUS</span></h1>"
                + "<span style='color: #94a3b8; font-size: 10px; font-weight: bold; letter-spacing: 2px; text-transform: uppercase;'>Carteira do Colecionador</span>"
                + "</div>"
                + "<div style='background-color: #ffffff; padding: 30px; border-radius: 16px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);'>"
                + "<h2 style='color: #1e293b; margin-top: 0;'>Olá, " + nomeUsuario + "! 👋</h2>"
                + "<p style='color: #475569; line-height: 1.6;'>Sua conta foi criada com sucesso! Estamos muito felizes em ter você conosco.</p>"
                + "<p style='color: #475569; line-height: 1.6;'>A partir de agora, você pode organizar seu catálogo geral, gerenciar seu estoque de repetidas e ter controle total do seu fluxo de caixa na coleção.</p>"
                + "<div style='text-align: center; margin: 35px 0;'>"
                + "<a href='https://copanexus.com.br' style='background-color: #102C57; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 14px; display: inline-block;'>Acessar Minha Coleção</a>"
                + "</div>"
                + "<p style='color: #475569; line-height: 1.6; margin-bottom: 5px;'>Bora completar esse álbum!</p>"
                + "<p style='color: #102C57; font-weight: 900; margin-top: 0;'>Equipe CopaNexus</p>"
                + "</div>"
                + "</div>";

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("CopaNexus <boasvindas@copanexus.com.br>")
                .to(para)
                .replyTo("copanexusofc@gmail.com")
                .subject("Bem-vindo ao CopaNexus! ⚽")
                .html(htmlMsg)
                .build();

        try {
            resend.emails().send(params);
            System.out.println("E-mail de boas-vindas enviado via API com sucesso para: " + para);
        } catch (ResendException e) {
            System.err.println("Erro ao enviar e-mail via API Resend: " + e.getMessage());
        }
    }

    @Async
    public void enviarEmailRecuperacaoSenha(String para, String codigo) {
        Resend resend = new Resend(apiKey);

        String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #F1F5F9; border-radius: 10px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'>"
                + "<h1 style='color: #102C57; font-size: 28px; margin: 0; font-weight: 900; letter-spacing: -1px;'>COPA<span style='color: #FE0000;'>NEXUS</span></h1>"
                + "</div>"
                + "<div style='background-color: #ffffff; padding: 30px; border-radius: 16px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); text-align: center;'>"
                + "<h2 style='color: #1e293b; margin-top: 0;'>Esqueceu sua senha?</h2>"
                + "<p style='color: #475569; line-height: 1.6;'>Recebemos um pedido para redefinir a senha da sua conta. Use o código de segurança abaixo para criar uma nova senha:</p>"
                + "<div style='margin: 30px 0;'>"
                + "<span style='background-color: #f8fafc; border: 2px dashed #cbd5e1; color: #102C57; font-size: 32px; font-weight: 900; padding: 15px 30px; border-radius: 12px; letter-spacing: 5px;'>" + codigo + "</span>"
                + "</div>"
                + "<p style='color: #ef4444; font-size: 12px; font-weight: bold;'>⚠️ Este código expira em 15 minutos.</p>"
                + "<p style='color: #94a3b8; font-size: 12px; margin-top: 20px;'>Se você não pediu para redefinir sua senha, pode ignorar este e-mail em segurança.</p>"
                + "</div>"
                + "</div>";

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("CopaNexus <suporte@copanexus.com.br>")
                .to(para)
                .replyTo("copanexusofc@gmail.com")
                .subject("Recuperação de Senha - CopaNexus 🔐")
                .html(htmlMsg)
                .build();

        try {
            resend.emails().send(params);
            System.out.println("E-mail de recuperação enviado via API com sucesso para: " + para);
        } catch (ResendException e) {
            System.err.println("Erro ao enviar e-mail de recuperação via API Resend: " + e.getMessage());
        }
    }
}