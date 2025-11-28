package io.fleetcoreplatform.Models;

import java.util.ArrayList;
import java.util.List;
import org.postgis.*;

public class OutpostAreaModel {
    public List<PolygonPoint2DModel> points;

    public OutpostAreaModel(List<PolygonPoint2DModel> points) {
        this.points = points;
    }

    public Geometry toGeometry() {
        ArrayList<Point> points = new ArrayList<>();
        for (PolygonPoint2DModel point : this.points) {
            points.add(new Point(point.x(), point.y()));
        }

        LinearRing[] lineString = new LinearRing[] {new LinearRing(points.toArray(new Point[0]))};
        return new Polygon(lineString);
    }
}
