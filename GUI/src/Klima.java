package GUI.src;

import java.awt.Point;
import java.text.DecimalFormat;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * The Klima class for the temperature display. Also contains the target
 * temperature value for the room the module is in.
 * 
 * @author MinhMax
 *
 */
public class Klima {

	private boolean heizungsstatus;
	private double zielTemp = 20; // Standard value for initialization

	private Image fire = new Image("/GUI/resources/fire.png", true);
	private Image temp = new Image("/GUI/resources/temp.png", true);
	private Image snow = new Image("/GUI/resources/snow.png", true);
	private Image perfect = new Image("/GUI/resources/perfect.png", true);
	private ImageView iv1 = new ImageView();
	private ImageView iv2 = new ImageView();
	private Label temps = new Label();
	private Button settings = new Button(null, new ImageView(temp));
	private Temppop tpop = null;
	private Raum raum;

	/*
	 * VBox and HBox for the temperature display
	 */
	private VBox vebox = new VBox();
	private HBox box = new HBox();

	public Klima(Point p, Raum r) throws Exception {
		raum = r;
		/*
		 * Scaling of the icons
		 */
		iv2.setFitWidth(50);
		iv2.setFitHeight(50);
		iv1.setFitHeight(21);
		iv1.setFitWidth(21);

		/*
		 * Thermometer button for opening the temperature pop-up
		 */
		iv2.setImage(temp);
		settings.setBackground(null);

		/*
		 * Positioning of the temperature display
		 */
		vebox.setLayoutX(p.x - 71);
		vebox.setLayoutY(p.y - 80);

		// Clickable icon for temperature pop-up
		settings.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				Temppop tk = new Temppop(raum);
				Stage stage = new Stage();
				stage.setTitle("Temperaturkonfiguartion");
				stage.initModality(Modality.WINDOW_MODAL);
				try {
					tk.display(stage);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (tpop != null) {
					// Do nothing
				} else {
					tpop = new Temppop(raum);
				}
			}
		});
	}

	/**
	 * Checks the given value and sets the temperature icon accordingly. Also
	 * sets the temperature label.
	 * 
	 * @param s
	 *            the value to be checked
	 */
	public void setImageAndLabel(String s) {
		if (s.equals("heiss")) {
			iv1.setImage(snow);
		} else if (s.equals("kalt")) {
			iv1.setImage(fire);
		} else if (s.equals("perfekt")) {
			iv1.setImage(perfect);
		}

		// Float format
		DecimalFormat df = new DecimalFormat("00.00");
		String tempZielFormat = df.format(raum.getModul().gettemperatur());

		// Set the temperature label
		temps.setText(tempZielFormat + "�C");
	}

	public ImageView getIv1() {
		return iv1;
	}

	public void setIv1(ImageView iv1) {
		this.iv1 = iv1;
	}

	public ImageView getIv2() {
		return iv2;
	}

	public void setIv2(ImageView iv2) {
		this.iv2 = iv2;
	}

	public VBox getVebox() {
		return vebox;
	}

	public void setVebox(VBox vebox) {
		this.vebox = vebox;
	}

	public HBox getBox() {
		return box;
	}

	public void setBox(HBox box) {
		this.box = box;
	}

	public Button getSettings() {
		return settings;
	}

	public void setSettings(Button settings) {
		this.settings = settings;
	}

	public Label getTemps() {
		return temps;
	}

	public void setTemps(Label temps) {
		this.temps = temps;
	}

	public boolean getHeizungsstatus() {
		return heizungsstatus;
	}

	public void setHeizungsstatus(boolean heizungsstatus) {
		this.heizungsstatus = heizungsstatus;
	}

	public double getZielTemp() {
		return zielTemp;
	}

	public void setZielTemp(double zielTemp) {
		this.zielTemp = zielTemp;
	}
}
