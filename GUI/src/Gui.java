package GUI.src;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * The Gui class contains the entire FXML based GUI including all
 * functionalities. The function getRaumListe() returns an ArrayList containing
 * all room objects.
 *
 * @see javafx.application.Application
 * @see Raum
 *
 * @author MinhMax & Gaitan
 *
 */
public class Gui extends Application {

	private static int lichtcounter = 0;

	// Schnittstelle
	private static Schnittstelle schnittstelle;
	private static String raumString;

	// Variables
	private static final int minRoomWidth = 88;
	private static final int minRoomHeight = 102;
	private double pressedX;
	private double pressedY;
	private double releasedX;
	private double releasedY;
	private static ArrayList<Rectangle> rectangles = new ArrayList<Rectangle>();
	private static ArrayList<Raum> raumListe = new ArrayList<Raum>(3);
	private static int idCounter = 1;
	private static int tempModulID = 0;
	private static Raum tempRaum = null;
	private static ObservableList<Node> list = FXCollections.observableArrayList();

	private static Scene scene;
	private static SplitPane splitpane;
	private static AnchorPane anchorpane;
	private static Canvas canvas, canvas2;
	private static GraphicsContext gc, gc2;
	private static Button reset;
	private static Label connectionStatus;
	String temperatur = "";
	Stage stage = new Stage();

	private static Button save;
	private static File file;

	private Button restore;
	private static boolean restoreMutEx = false;

	@Override
	/**
	 * Thread for communication with the WSN, handles connection / reconnection
	 * of the controller as well as reading data from the WSN
	 */
	public void start(Stage primaryStage) throws Exception {

		/*
		 * create the AnchorPane and load the Path of the FXML File
		 */
		AnchorPane root = (AnchorPane) FXMLLoader.load(getClass().getResource("RoomView.fxml"));

		// Adding the AnchorPane into a scene
		scene = new Scene(root);

		ObservableList<Node> obs = root.getChildren();
		splitpane = (SplitPane) obs.get(0);

		/*
		 * Add canvases one and two to the right SplitPane
		 */
		anchorpane = (AnchorPane) splitpane.getItems().get(1);
		canvas = (Canvas) anchorpane.getChildren().get(1);
		canvas2 = new Canvas(554, 746);
		anchorpane.getChildren().add(canvas2);
		connectionStatus = (Label) anchorpane.getChildren().get(4);

		// Canvas for rooms
		gc = canvas.getGraphicsContext2D();
		// Canvas for room preview
		gc2 = canvas2.getGraphicsContext2D();

		/**
		 * Interface
		 */
		schnittstelle = new Schnittstelle();

		new Thread(new Runnable() {
			public void run() {
				// Establish connection
				while (Schnittstelle.getSerialPort() == null) {
					// Label for connection status
					connectionStatus.setText("Keine Verbindung!");

					schnittstelle.connect();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
						ie.printStackTrace();
					}
				}

				// Loop for receiving data
				while (Schnittstelle.getSerialPort().isOpened()) {

					// Receive data
					raumString = schnittstelle.receive();
					// update room data
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							updateRoom(raumString);
							// Label for connection status
							connectionStatus.setText("Verbunden");
						}
					});

					// Pause until the next data receive cycle
					try {
						Thread.sleep(500);
					} catch (InterruptedException ie) {
						ie.printStackTrace();
					}
				}

			}
		}).start();

		// Event handler for terminating the thread and connection upon closing
		// the GUI
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				schnittstelle.close();
				Platform.exit();
				System.exit(0);
			}
		});

		/*
		 * Sets the starting X and Y coordinates for the point when the mouse is
		 * clicked and dragged.
		 */
		anchorpane.setOnMousePressed(new EventHandler<MouseEvent>() {

			public void handle(MouseEvent event) {
				pressedX = event.getX();
				pressedY = event.getY();
			}
		});

		/*
		 * Sets the end point X and Y coordinates when the mouse is released.
		 */
		anchorpane.setOnMouseReleased(new EventHandler<MouseEvent>() {

			public void handle(MouseEvent event) {
				gc2.clearRect(0, 0, 554, 746);
				releasedX = event.getX();
				releasedY = event.getY();
				drawRectangle(gc);
			}
		});

		/*
		 * Preview for room drawing
		 */
		anchorpane.setOnMouseDragged(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent event) {
				// Clears the canvas every tick so the preview is dynamic
				gc2.clearRect(0, 0, 554, 746);

				Point p = new Point();
				p.setLocation(event.getX(), event.getY());

				double x, y, w, h;
				Rectangle tempTangle = new Rectangle();
				Boolean intersects = false;

				if (event.getX() > pressedX && event.getY() > pressedY) {
					// Upper left to lower right
					w = event.getX() - pressedX;
					h = event.getY() - pressedY;
					x = event.getX() - w;
					y = pressedY;
				} else if (pressedX > event.getX() && pressedY > event.getY()) {
					// Lower right to upper left
					w = pressedX - event.getX();
					h = pressedY - event.getY();
					x = pressedX - (pressedX - event.getX());
					y = event.getY();
				} else if (pressedX > event.getX() && pressedY < event.getY()) {
					// Upper right to lower left
					w = pressedX - event.getX();
					h = event.getY() - pressedY;
					x = event.getX();
					y = event.getY() - (event.getY() - pressedY);
				} else {
					// Lower left to upper right
					w = event.getX() - pressedX;
					h = pressedY - event.getY();
					x = event.getX() - (event.getX() - pressedX);
					y = pressedY - (pressedY - event.getY());
				}

				tempTangle.x = (int) x;
				tempTangle.y = (int) y;
				tempTangle.width = (int) w;
				tempTangle.height = (int) h;

				// Red preview if the drawn room is invalid
				for (Rectangle r : rectangles) {
					if (r.intersects(tempTangle)) {
						intersects = true;
						break;
					}
				}

				if (tempTangle.width < minRoomWidth || tempTangle.height < minRoomHeight) {
					gc2.setStroke(Color.RED);
				} else if ((event.getX() > 554 || event.getX() < 0) || (event.getY() > 746 || event.getY() < 0)) {
					gc2.setStroke(Color.RED);
				} else if (intersects) {
					gc2.setStroke(Color.RED);
				} else {
					gc2.setStroke(Color.WHITE);
				}

				gc2.strokeRect(x, y, w, h);
				event.consume();
			}

		});

		/*
		 * Drag and Drop feature. Starting the Drag-and-Drop Gesture on every
		 * child of the VBox except from the logo.
		 */
		// Hard-coded list of all the dragable items on the left pane
		VBox vbox = (VBox) splitpane.getItems().get(0);
		list.add(vbox.getChildren().get(1));
		list.add(vbox.getChildren().get(2));
		list.add(vbox.getChildren().get(3));
		list.add(vbox.getChildren().get(4));

		// Loop for determining which module or if a light is being dragged
		for (Node n : list) {
			n.setOnDragDetected(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					Dragboard db = n.startDragAndDrop(TransferMode.MOVE);
					ClipboardContent content = new ClipboardContent();
					content.putString("Hallo");
					db.setContent(content);
					if (n.getId() == list.get(0).getId()) {
						tempModulID = 1;
						// dropping modul 1
						Image image = new Image("/GUI/resources/modul1.png");
						db.setDragView(image);
					} else if (n.getId() == list.get(1).getId()) {
						tempModulID = 2;
						// dropping modul 2
						Image image = new Image("/GUI/resources/modul2.png");
						db.setDragView(image);
					} else if (n.getId() == list.get(2).getId()) {
						tempModulID = 3;
						// dropping modul 3
						Image image = new Image("/GUI/resources/modul3.png");
						db.setDragView(image);
					} else if (n.getId() == list.get(3).getId()) {
						tempModulID = 0;
						Image image = new Image("/GUI/resources/light.png");
						db.setDragView(image);
					}

					// Handling illegal drop positions (\)
					anchorpane.setOnDragOver(new EventHandler<DragEvent>() {
						@Override
						public void handle(DragEvent event) {
							Point p = new Point();
							p.setLocation(event.getX(), event.getY());
							for (Raum r : raumListe) {
								if (tempModulID != 0) {
									if (r.getRect().contains(p) && r.getModul() == null) {
										tempRaum = r;
										event.acceptTransferModes(TransferMode.MOVE);
									}
								} else {
									if (r.getRect().contains(p) && r.getModul() != null && r.getLicht() == null) {
										tempRaum = r;
										event.acceptTransferModes(TransferMode.MOVE);
									}
								}
							}

							// Handling the drop and adding new objects to the
							// room as well as making
							// already used modules unavailable
							anchorpane.setOnDragDropped(new EventHandler<DragEvent>() {
								@Override
								public void handle(DragEvent event) {
									Point p = new Point();
									p.setLocation(event.getX(), event.getY());

									if (tempRaum.getRect().contains(p)) {
										if (tempModulID == 0) {
											// Adding a light to a room
											tempRaum.setLicht(new Licht(p, tempRaum, anchorpane));
											createLichtAnzeige(tempRaum);
											System.out.println("Licht zu Raum " + tempRaum.getID() + " hinzugefuegt!");
											lichtcounter++;
											if (lichtcounter >= 3) {
												list.get(3).setOpacity(0.5);
												list.get(3).setDisable(true);
											}
											tempRaum = null;
										} else {
											// Creates the room
											tempRaum.setModul(new Modul(tempModulID));
											System.out.println(
													"Modul " + tempRaum.getModul().getModulID() + " hinzugefuegt!");
											// Adds the temperature display
											createTempAnzeige(tempRaum);

											// System.out.println("Raum: " +
											// tempRaum.getID() + ", "
											// + tempRaum.getposition_x() + ", "
											// + tempRaum.getposition_y());

											tempRaum.getKlima().setImageAndLabel(tempRaum.getModul()
													.temperaturanzeige((float) tempRaum.getKlima().getZielTemp()));
											// Fill the room with white
											gc.setFill(Color.WHITE);
											gc.fillRect(tempRaum.getRect().getX() + 1, tempRaum.getRect().getY() + 1,
													tempRaum.getRect().getWidth() - 2,
													tempRaum.getRect().getHeight() - 2);
											// Deactivating already used modules
											switch (tempModulID) {
											case 1:
												list.get(0).setOpacity(0.2);
												list.get(0).setDisable(true);
												// Informing the WSN about an
												// activated module
												updateModule(tempRaum);
												break;
											case 2:
												list.get(1).setOpacity(0.2);
												list.get(1).setDisable(true);
												// Informing the WSN about an
												// activated module
												updateModule(tempRaum);
												break;
											case 3:
												list.get(2).setOpacity(0.2);
												list.get(2).setDisable(true);
												// Informing the WSN about an
												// activated module
												updateModule(tempRaum);
												break;
											}
											// Reset temporary variables
											tempModulID = 0;
											tempRaum = null;
										}
									}
									event.setDropCompleted(true);
									event.consume();
								}
							});

							event.consume();
						}
					});

					event.consume();
				}
			});
		}

		/*
		 * Reset Button
		 */
		reset = (Button) anchorpane.getChildren().get(0);

		reset.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				/*
				 * reseting the current canvas.
				 */
				reset();
			}
		});

		/*
		 * save Button
		 */
		save = (Button) anchorpane.getChildren().get(2);

		save.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				// TODO Auto-generated method stub

				try {
					
					// Mutual exclusion of Restore and Reset
					if (!restoreMutEx) {
						restoreMutEx = true;
						restore.setDisable(true);
						restore.setOpacity(0.2);
					}


					/*
					 * initialing the file
					 */
					file = new File("gui/src/save.txt");

					/*
					 * check if the file exist or not. and create it if not.
					 */

					if (!file.exists()) {
						file.createNewFile();
					}

					FileWriter fwriter = new FileWriter(file);
					PrintWriter pwriter = new PrintWriter(fwriter);
					/*
					 * Werden gespeichert: Raum_ID, Raum_X , Raum_Y Licht_X ,
					 * Licht_Y, Lichts_status, Licht_getLichtZielWert() sowie
					 * Klima_ZielTemp und Klima_Heizungsstatus
					 */

					System.out.println("raumListe size :" + raumListe.size());
					System.out.println("size of rectangles : " + rectangles.size());

					for (int i = 0; i < raumListe.size(); i++) {
						if (raumListe.get(i).getLicht() == null & raumListe.get(i).getModul() == null) {
							String string = raumListe.get(i).getRect().getX() + ";" + raumListe.get(i).getRect().getY()
									+ ";" + raumListe.get(i).getRect().getWidth() + ";"
									+ raumListe.get(i).getRect().getHeight() + ";" + raumListe.get(i).getID() + ";"
									+ raumListe.get(i).getposition_x() + ";" + raumListe.get(i).getposition_y();
							pwriter.println(string);
						} else if (raumListe.get(i).getLicht() == null) {
							String string = raumListe.get(i).getRect().getX() + ";" + raumListe.get(i).getRect().getY()
									+ ";" + raumListe.get(i).getRect().getWidth() + ";"
									+ raumListe.get(i).getRect().getHeight() + ";" + raumListe.get(i).getID() + ";"
									+ raumListe.get(i).getposition_x() + ";" + raumListe.get(i).getposition_y() + ";"
									+ raumListe.get(i).getKlima().getHeizungsstatus() + ";"
									+ raumListe.get(i).getModul().getModulID() + ";"
									+ raumListe.get(i).getKlima().getZielTemp() + ";"
									+ raumListe.get(i).getKlima().getTemps().getText();

							pwriter.println(string);

						} else {
							String string = raumListe.get(i).getRect().getX() + ";" + raumListe.get(i).getRect().getY()
									+ ";" + raumListe.get(i).getRect().getWidth() + ";"
									+ raumListe.get(i).getRect().getHeight() + ";"
									+ raumListe.get(i).getLicht().getLichtPoint().getX() + ";"
									+ raumListe.get(i).getLicht().getLichtPoint().getY() + ";"
									+ raumListe.get(i).getID() + ";" + raumListe.get(i).getposition_x() + ";"
									+ raumListe.get(i).getposition_y() + ";"
									+ raumListe.get(i).getLicht().getLichtAnAus() + ";"
									+ raumListe.get(i).getLicht().getLichtModus() + ";"
									+ raumListe.get(i).getKlima().getHeizungsstatus() + ";"
									+ raumListe.get(i).getModul().getModulID() + ";"
									+ raumListe.get(i).getLicht().getLichtZielWert() + ";"
									+ raumListe.get(i).getKlima().getZielTemp() + ";"
									+ raumListe.get(i).getKlima().getTemps().getText();

							pwriter.println(string);

						}
						pwriter.flush();

					}
					pwriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		});
		/*
		 * restoring the data.
		 */
		restore = (Button) anchorpane.getChildren().get(3);

		restore.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				/**
				 * first reset the canvas again if it wasn't already done.
				 */
				// reset();
				// TODO Auto-generated method stub
				try {
					file = new File("gui/src/save.txt");
					FileReader filereader = new FileReader(file);
					BufferedReader bufreader = new BufferedReader(filereader);
					String string = bufreader.readLine();

					// Mutual exclusion of Restore and Reset
					if (!restoreMutEx) {
						restoreMutEx = true;
						restore.setDisable(true);
						restore.setOpacity(0.2);
					}

					gc.setLineDashes(8);
					gc.setLineWidth(3);
					gc.setStroke(Color.LIGHTGREY);
					int numberOfmodule = 0;
					while (string != null) {
						String[] arr = string.split(";");

						if (arr.length == 7) {

							Rectangle old_viereck = new Rectangle();
							old_viereck.setRect(Double.parseDouble(arr[0]), Double.parseDouble(arr[1]),
									Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
							// Draw room because of viability

							gc.strokeRect(old_viereck.x, old_viereck.y, old_viereck.width, old_viereck.height);
							rectangles.add(old_viereck);
							Raum old_raum = new Raum(Integer.parseInt(arr[4]), old_viereck.x + old_viereck.width,
									old_viereck.y + old_viereck.height, old_viereck);
							raumListe.add(old_raum);
							gc2.setStroke(Color.LIGHTGREY);

						} else if (arr.length == 11) {

							Rectangle old_viereck = new Rectangle();
							old_viereck.setRect(Double.parseDouble(arr[0]), Double.parseDouble(arr[1]),
									Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));

							gc.strokeRect(old_viereck.x, old_viereck.y, old_viereck.width, old_viereck.height);
							rectangles.add(old_viereck);
							Raum old_raum = new Raum(Integer.parseInt(arr[4]), old_viereck.x + old_viereck.width,
									old_viereck.y + old_viereck.height, old_viereck);

							gc2.setStroke(Color.LIGHTGREY);
							gc.setFill(Color.WHITE);
							gc.fillRect(old_raum.getRect().getX() + 1, old_raum.getRect().getY() + 1,
									old_raum.getRect().getWidth() - 2, old_raum.getRect().getHeight() - 2);

							Modul modul = new Modul(Integer.parseInt(arr[8]));

							old_raum.setModul(modul);

							System.out.println("OLD_Modul " + modul.getModulID() + " hinzugefuegt!");

							// FUEGT TEMPERATURANZEIGE HINZU

							createTempAnzeige(old_raum);
							old_raum.getKlima()
									.setImageAndLabel(old_raum.getModul().temperaturanzeige(Float.parseFloat(arr[9])));
							old_raum.getKlima().setHeizungsstatus(Boolean.parseBoolean(arr[7]));
							old_raum.getKlima().setZielTemp(Double.parseDouble(arr[9]));
							old_raum.getKlima().getTemps().setText(arr[10]);

							// MALE RAUM AUS
							gc.setFill(Color.WHITE);
							gc.fillRect(old_raum.getRect().getX() + 1, old_raum.getRect().getY() + 1,
									old_raum.getRect().getWidth() - 2, old_raum.getRect().getHeight() - 2);

							raumListe.add(old_raum);
							rectangles.add(old_viereck);
							System.out.println("ModulID  :" + modul.getModulID());
							/*
							 * Deaktivierung von benutzten Modulen.
							 */

							switch (modul.getModulID()) {
							case 1:
								list.get(0).setOpacity(0.2);
								list.get(0).setDisable(true);
								break;
							case 2:
								list.get(1).setOpacity(0.2);
								list.get(1).setDisable(true);
								break;
							case 3:
								list.get(2).setOpacity(0.2);
								list.get(2).setDisable(true);
								break;
							}

							// Info fuer das WSN zu neu aktiviertem Modul
							// updateModule(old_raum);

						} else if (arr.length == 16) {

							Rectangle old_viereck = new Rectangle();
							old_viereck.setRect(Double.parseDouble(arr[0]), Double.parseDouble(arr[1]),
									Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
							// Draw room because of viability

							gc.strokeRect(old_viereck.x, old_viereck.y, old_viereck.width, old_viereck.height);
							rectangles.add(old_viereck);
							System.out.println("IDDD: " + Integer.parseInt(arr[6]));
							Raum old_raum = new Raum(Integer.parseInt(arr[6]), old_viereck.x + old_viereck.width,
									old_viereck.y + old_viereck.height, old_viereck);

							System.out.println("Boolean.parseBoolean(arr[11]) :: " + Boolean.parseBoolean(arr[9]));

							gc2.setStroke(Color.LIGHTGREY);

							Point point = new Point();
							point.setLocation(Double.parseDouble(arr[4]), Double.parseDouble(arr[5]));
							Licht licht = new Licht(point, old_raum, anchorpane);
							licht.setLichtAnAus(Boolean.parseBoolean(arr[9]));
							licht.setLichtModus(Boolean.parseBoolean(arr[10]));
							licht.setLichtZielWert(Integer.parseInt(arr[13]));
							old_raum.setLicht(licht);
							createLichtAnzeige(old_raum);
							System.out.println("Licht zu Raum " + old_raum.getID() + " hinzugefuegt!");

							Modul modul = new Modul(old_raum.getID());
							System.out.println(modul.getModulID() + "old_raum.getId");
							old_raum.setModul(modul);

							System.out.println("OLD_Modul " + modul.getModulID() + " hinzugefuegt!");

							// FUEGT TEMPERATURANZEIGE HINZU
							createTempAnzeige(old_raum);

							old_raum.getKlima()
									.setImageAndLabel(old_raum.getModul().temperaturanzeige(Float.parseFloat(arr[14])));
							old_raum.getKlima().setHeizungsstatus(Boolean.parseBoolean(arr[11]));
							old_raum.getKlima().setZielTemp(Double.parseDouble(arr[14]));
							old_raum.getKlima().getTemps().setText(arr[15]);

							// MALE RAUM AUS
							gc.setFill(Color.WHITE);
							gc.fillRect(old_raum.getRect().getX() + 1, old_raum.getRect().getY() + 1,
									old_raum.getRect().getWidth() - 2, old_raum.getRect().getHeight() - 2);

							raumListe.add(old_raum);
							rectangles.add(old_viereck);

							/*
							 * Deaktivierung von benutzten Modulen.
							 */

							switch (Integer.parseInt(arr[12])) {
							case 1:
								list.get(0).setOpacity(0.2);
								list.get(0).setDisable(true);
								break;
							case 2:
								list.get(1).setOpacity(0.2);
								list.get(1).setDisable(true);
								break;
							case 3:
								list.get(2).setOpacity(0.2);
								list.get(2).setDisable(true);
								break;
							}
							numberOfmodule++;
							// Info fuer das WSN zu neu aktiviertem Modul
							// updateModule(old_raum);

						}

						// Update Modules in the WSN ----------
						new Thread(new Runnable() {
							public void run() {
								try {
									// Wait for reset to finish
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								for (int i = 0; i < raumListe.size(); i++) {
									if (raumListe.get(i).getModul() != null) {
										updateModule(raumListe.get(i));

										try {
											// Wait in between messages
											Thread.sleep(300);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}
								}
							}
						}).start();
						// ------------------------------------

						string = bufreader.readLine();
					}
					if (numberOfmodule == 3) {
						list.get(3).setOpacity(0.2);
						list.get(3).setDisable(true);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		primaryStage.setScene(scene);

		// Set the title of the stage
		primaryStage.setTitle("SmartAssist");

		// Locks window size
		primaryStage.setResizable(false);

		// Display the stage
		primaryStage.show();
	}

	/**
	 * Upon dragging a module into a room and activating it crates the
	 * temperature display and button in a room that later can be clicked to
	 * open up the temperature pop-up.
	 * 
	 * @param r
	 *            the Room the module and temperature display should be added to
	 */
	private void createTempAnzeige(Raum r) {

		Label raumname = new Label();
		// If the room doesn't already have a module added to it
		if (r.getModul() != null) {

			String temperatur = r.getModul().temperaturanzeige(22.00f);
			r.getKlima().setImageAndLabel(temperatur);

			// Adds a label to the activated room
			if (r.getModul().getModulID() == 1) {
				raumname.setText("Zimmer 1");
			} else if (r.getModul().getModulID() == 2) {
				raumname.setText("Zimmer 2");
			} else if (r.getModul().getModulID() == 3) {
				raumname.setText("Zimmer 3");
			}
			raumname.setTextFill(Color.DIMGREY);

			raumname.setLayoutX(r.getRect().getX() + 5);
			raumname.setLayoutY(r.getRect().getY());
			anchorpane.getChildren().add(raumname);

			// Adds the temperature display and button
			anchorpane.getChildren().add(r.getKlima().getVebox());
			// System.out.println("MinX :
			// "+r.getKlima().getVebox().getBoundsInParent().get);
			// TemperaturIcon
			r.getKlima().getVebox().getChildren().add(r.getKlima().getSettings());
			r.getKlima().getVebox().getChildren().add(r.getKlima().getBox());
			// Current temperature
			r.getKlima().getBox().getChildren().add(r.getKlima().getTemps());
			// Current icon
			r.getKlima().getBox().getChildren().add(r.getKlima().getIv1());
		}
	}

	/**
	 * Upon dragging a light into a room creates the light display and button
	 * that later can be clicked to open up the light pop-up.
	 * 
	 * @param r
	 *            the Room the light should be added to
	 */
	private void createLichtAnzeige(Raum r) {
		anchorpane.getChildren().add(r.getLicht().getVebox());
		// LichtIcon
		r.getLicht().getVebox().getChildren().add(r.getLicht().getSettings());
		r.getLicht().getVebox().getChildren().add(r.getLicht().getBox());
	}

	/**
	 * Draws a visible rectangle and creates a room object. Drawn rooms must be
	 * at least 88x102 pixels big and must not intersect with another room.
	 *
	 * @see Raum
	 * @param gc
	 *            the GraphicsContext the rectangle will be drawn into
	 */
	private void drawRectangle(GraphicsContext gc) {

		// Calculate height and width
		Rectangle viereck = new Rectangle();

		if (releasedX > pressedX && releasedY > pressedY) {
			// Upper left to lower right
			viereck.width = (int) (releasedX - pressedX);
			viereck.height = (int) (releasedY - pressedY);
			viereck.x = (int) (pressedX);
			viereck.y = (int) (pressedY);
		} else if (pressedX > releasedX && pressedY > releasedY) {
			// Lower right to upper left
			viereck.width = (int) (pressedX - releasedX);
			viereck.height = (int) (pressedY - releasedY);
			viereck.x = (int) (pressedX - viereck.width);
			viereck.y = (int) (releasedY);
		} else if (pressedX > releasedX && pressedY < releasedY) {
			// Upper right to lower left
			viereck.width = (int) (pressedX - releasedX);
			viereck.height = (int) (releasedY - pressedY);
			viereck.x = (int) (releasedX);
			viereck.y = (int) (releasedY - viereck.height);
		} else {
			// Lower left to upper right
			viereck.width = (int) (releasedX - pressedX);
			viereck.height = (int) (pressedY - releasedY);
			viereck.x = (int) (releasedX - viereck.width);
			viereck.y = (int) (pressedY - viereck.height);
		}

		// Set graphical options for the drawn rectangles
		gc.setLineDashes(8);
		gc.setLineWidth(3);
		gc.setStroke(Color.LIGHTGREY);

		/*
		 * Check if drawn room is viable (big enough, not intersecting, within
		 * canvas bounds)
		 */
		Boolean intersect = false;
		if (viereck.width < minRoomWidth || viereck.height < minRoomHeight) {
			intersect = true;
			System.out.println("Room too small!");
		}
		if ((releasedX > 554 || releasedX < 0) || (releasedY > 746 || releasedY < 0)) {
			intersect = true;
			System.out.println("Rooms cannot be out of bounds!");
		}
		for (Rectangle r : rectangles) {
			if (r.intersects(viereck)) {
				intersect = true;
				System.out.println("Rooms cannot intersect!");
				break;
			}
		}

		// Draw and create room if viable
		if (!intersect) {
			gc.strokeRect(viereck.x, viereck.y, viereck.width, viereck.height);
			// Determine the lower right point of a room (used by the
			// temperature display)
			raumListe.add(new Raum(idCounter, viereck.x + viereck.width, viereck.y + viereck.height, viereck));
			rectangles.add(viereck);
			// System.out.println("Raum: " + raumListe.get(idCounter -
			// 1).getID() + ", "
			// + raumListe.get(idCounter - 1).getposition_x() + ", "
			// + raumListe.get(idCounter - 1).getposition_y());
			idCounter++;
		}

		System.out.println("Breite: " + viereck.width);
		System.out.println("Hoehe: " + viereck.height);
	}

	/**
	 * Processes the room string that is received from the WSN.
	 * 
	 * @param raumString
	 *            the room string from the WSN
	 */
	public void updateRoom(String raumString) {
		String[] raumStringArray = raumString.split(";");

		// "ID;ModuleStatus;LightMode;ClimateMode;LightStatus;LightValue;Temperature"

		// Read the room data
		int raumId = Integer.parseInt(raumStringArray[0]);
		boolean lichtModus = "1".equals(raumStringArray[2]);
		boolean tempStatus = "1".equals(raumStringArray[3]);
		boolean lichtStatus = "1".equals(raumStringArray[4]);
		int lichtWert = Integer.parseInt(raumStringArray[5]);
		float tempWert = Float
				.parseFloat(raumStringArray[6].substring(0, 2) + "." + raumStringArray[6].substring(2, 4));

		// Assign the data to the correct room and update it
		for (Raum r : raumListe) {
			if (r.getModul() != null && r.getModul().getModulID() == raumId) {
				r.getModul().setlichtwert(lichtWert);
				r.getKlima().setHeizungsstatus(tempStatus);
				r.getModul().settemperatur(tempWert);
				r.getKlima().setImageAndLabel(r.getModul().temperaturanzeige((float) r.getKlima().getZielTemp()));
				if (r.getLicht() != null) {
					r.getLicht().setLichtModus(lichtModus);
					r.getLicht().setLichtAnAus(lichtStatus);
				}
				break;
			}
		}

	}

	/**
	 * Sends the current data and settings of a module in a room to the WSN.
	 *
	 * @param Raum
	 *            the room with the module that should be updated in the WSN
	 */
	public static void updateModule(Raum raum) {

		// "ID;ModuleStatus;LightMode;ClimateMode;LightStatus;TargetLightValue;TargetTemperatureValueE"

		if (raum.getModul() != null) {
			String moduleId = Integer.toString(raum.getModul().getModulID()) + ";";
			String moduleStatus = "1;"; // Active module is active
			String lichtModus;
			String tempStatus;
			String lichtStatus;
			String lichtZiel = "000;";

			if (raum.getLicht() != null) {
				if (raum.getLicht().getLichtModus()) {
					lichtModus = "1;";
				} else {
					lichtModus = "0;";
				}

				if (raum.getLicht().getLichtAnAus()) {
					lichtStatus = "1;";
				} else {
					lichtStatus = "0;";
				}

				// Translate target light value from 0-3 to 0-255
				if (raum.getLicht().getLichtZielWert() == 0) {
					lichtZiel = ("000;");
				}
				if (raum.getLicht().getLichtZielWert() == 1) {
					lichtZiel = ("085;");
				}
				if (raum.getLicht().getLichtZielWert() == 2) {
					lichtZiel = ("170;");
				}
				if (raum.getLicht().getLichtZielWert() == 3) {
					lichtZiel = ("255;");
				}

			} else {
				lichtStatus = "0;";
				lichtModus = "0;";
			}

			if (raum.getKlima().getHeizungsstatus()) {
				tempStatus = "1;";
			} else {
				tempStatus = "0;";
			}

			// Set the format for the target temperature
			DecimalFormat df = new DecimalFormat("00.00");
			String tempZielFormat = df.format(raum.getKlima().getZielTemp());
			String tempZiel = tempZielFormat.substring(0, 2) + tempZielFormat.substring(3, 5) + "E";

			// Assemble the string and send it to the WSN
			schnittstelle.send(moduleId + moduleStatus + lichtModus + tempStatus + lichtStatus + lichtZiel + tempZiel);
		}
	}

	/*
	 * reset the canvas.
	 */
	public static void reset() {
		// Mutual exclusion of Restore and Reset

		if (restoreMutEx) {
			anchorpane.getChildren().get(3).setOpacity(1);
			anchorpane.getChildren().get(3).setDisable(false);
			restoreMutEx = false;
		}

		// Clear the canvases
		gc.clearRect(0, 0, 554, 746);
		gc2.clearRect(0, 0, 554, 746);

		// Informing the WSN about deactivated modules
		new Thread(new Runnable() {
			public void run() {
				for (int i = 0; i < raumListe.size(); i++) {
					if (raumListe.get(i).getModul() != null) {
						schnittstelle.send(raumListe.get(i).getModul().getModulID() + ";0;0;0;0;000;0000E");
						try {
							// Wait in between messages
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				// Clear the room list
				raumListe.clear();
			}
		}).start();

		// Clear the rectangle list
		rectangles.clear();

		// Reset the counter variables
		idCounter = 1;
		lichtcounter = 0;

		// Remove the light and temperature buttons
		anchorpane.getChildren().subList(6, anchorpane.getChildren().size()).clear();

		// Make the modules active and interactable again
		for (Node n : list) {
			if (n.isDisable() == true) {
				n.setDisable(false);
				n.setOpacity(1);
			}
		}
	}

	/**
	 * Getter for the room ArrayList
	 * 
	 * @return the ArrayList of all rooms
	 */
	public static ArrayList<Raum> getRaumListe() {
		return raumListe;
	}

}
