package cloudgene.mapred.util;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;

public class MailUtil {

	private static final Logger log = LoggerFactory.getLogger(MailUtil.class);

	public static void notifyAdmin(Settings settings, String subject, String text) throws Exception {

		if (settings.getAdminMail() != null && !settings.getAdminMail().isEmpty()) {

			send(settings.getMail().get("smtp"), settings.getMail().get("port"), settings.getMail().get("user"),
					settings.getMail().get("password"), settings.getMail().get("name"), settings.getAdminMail(),
					subject, text);
		}

	}

	public static void notifySlack(Settings settings,
			String text){
		if (settings.getSlack() != null && !settings.getSlack().isEmpty()){
			SlackApi api = new SlackApi(settings.getSlack());
			SlackMessage message = new SlackMessage("Cloudgene", text);
			message.setIcon(":rocket:");
			api.call(message);
			log.debug("Sent slack notification");
		}
	}

	public static void send(Settings settings, String tos, String subject, String text) throws Exception {

		send(settings.getMail().get("smtp"), settings.getMail().get("port"), settings.getMail().get("user"),
				settings.getMail().get("password"), settings.getMail().get("name"), tos, subject, text);

	}

	public static void send(final String smtp, final String port, final String username, final String password,
			final String name, String tos, String subject, String text) throws Exception {

		Properties props = new Properties();
		props.put("mail.smtp.host", smtp);
		props.put("mail.smtp.port", port);

		Session session = null;

		if (username != null && !username.isEmpty()) {

			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");
			session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			});
		} else {

			session = Session.getInstance(props);

		}

		try {

			InternetAddress[] addresses = InternetAddress.parse(tos);

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(name));
			message.setRecipients(Message.RecipientType.TO, addresses);
			message.setSubject(subject);
			message.setText(text);

			Transport.send(message);

			log.debug("E-Mail sent to " + tos + ".");

		} catch (MessagingException e) {
			throw new Exception("mail could not be sent: " + e.getMessage());
		}
	}
}
