/**
 * Thrown when a fixed-size array (friends or items) is already full
 * and no more elements can be added.
 */
public class FriendGroupFullException extends Exception {
    public FriendGroupFullException(String message) {

        super(message);
    }
}