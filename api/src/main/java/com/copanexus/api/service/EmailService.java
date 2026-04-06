package com.copanexus.api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarEmailBoasVindas(String para, String nomeUsuario) {
        try {
            // Cria um e-mail "complexo" que suporta HTML
            MimeMessage message = mailSender.createMimeMessage();

            // O 'true' indica que vamos enviar um e-mail multipart (HTML/Anexos)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("nao-responda@copanexus.com.br");
            helper.setTo(para);
            helper.setSubject("Bem-vindo ao CopaNexus! ⚽");

            // Corpo do e-mail escrito em HTML com CSS inline
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
                    + "<a href='http://localhost:5173' style='background-color: #102C57; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 14px; display: inline-block;'>Acessar Minha Coleção</a>"
                    + "</div>"
                    + "<p style='color: #475569; line-height: 1.6; margin-bottom: 5px;'>Bora completar esse álbum!</p>"
                    + "<p style='color: #102C57; font-weight: 900; margin-top: 0;'>Equipe CopaNexus</p>"
                    + "</div>"
                    + "</div>";

            // O 'true' aqui no final avisa o Spring Boot que essa String é um HTML e não um texto comum
            helper.setText(htmlMsg, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            System.err.println("Erro ao montar o e-mail HTML: " + e.getMessage());
        }
    }
}