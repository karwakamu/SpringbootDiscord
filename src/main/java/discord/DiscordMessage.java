package discord;

public class DiscordMessage
{
    private String channel;
    private String user;
    private String content;

    public DiscordMessage(String channel, String user, String content)
    {
        this.channel = channel;
        this.user = user;
        this.content = content;
    }

    public String GetChannel() {return channel;};
    public String GetUser() {return user;};
    public String GetContent() {return content;};
}