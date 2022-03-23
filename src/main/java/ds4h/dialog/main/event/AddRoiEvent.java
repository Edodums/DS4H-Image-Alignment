package ds4h.dialog.main.event;

import java.awt.*;

public class AddRoiEvent implements IMainDialogEvent {
    private final Point coordinates;
    public AddRoiEvent(Point coordinates) {
        this.coordinates = coordinates;
    }

    public Point getClickCoordinates() {
        return this.coordinates;
    }
}
