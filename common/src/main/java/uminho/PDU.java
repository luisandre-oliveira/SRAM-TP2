package uminho;

import java.io.Serializable;

public class PDU implements Serializable {
    private int F;
    private final int A;
    private final String Message;

    public PDU(int F, int A, String Message) {
        this.F = F;
        this.A = A;
        this.Message = Message;
    }

    public PDU() {
        this.F = 0;
        this.A = 0;
        this.Message = null;
    }

    public int getF() {
        return F;
    }

    public int getA() {
        return A;
    }

    public String getMessage() {
        return Message;
    }

    @Override
    public String toString() {
        return F + "|" + A + "|" + Message;
    }

    public static PDU fromString(String data) {
        int F, A; String message;
        String[] parts = data.split("\\|", 3);
        F = Integer.parseInt(parts[0]);
        A = Integer.parseInt(parts[1]);
        message = parts[2];
        return new PDU(F, A, message);
    }
}
