package com.aksum;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import javax.swing.table.TableCellEditor;

public class Main extends ApplicationAdapter {
	private static final int SCR_WIDTH = 1700,SCR_HEIGHT = 1000; // World's size
	private static final int CELL_SIZE = 10; // Size of one cell
	public static final int FIELD_WIDTH = SCR_WIDTH/CELL_SIZE, FIELD_HEIGHT = SCR_HEIGHT/CELL_SIZE;
	public static final int GENOME_SIZE = 64;
	private static final int BASIC_VIEW = 0,ENERGY_VIEW = 1,NUTRITION_VIEW = 2;
	public static int killed = 0;
	private ShapeRenderer shapeRenderer;
	int viewState = 0;
	SpriteBatch batch;
	OrthographicCamera camera;
	Stage stage;

	Texture imgMolecule;

	BitmapFont font;
	BitmapFont smallFont;

	TextButton basicViewButton;
	TextButton energyViewButton;
	TextButton nutritionViewButton;

	TextButton saveButton;
	TextButton loadButton;
	String loadFileName;
	String saveFilePath = "saves\\";

	TextButton.TextButtonStyle textButtonStyle;
	Table table;
	Container<ScrollPane> tableContainer;

	public static final int[][] field = new int[FIELD_WIDTH][FIELD_HEIGHT]; // -1 - Никого,-2 - труп, иначе индекс молекулы
	public static final int[][] fieldSun = new int[FIELD_WIDTH][FIELD_HEIGHT];
	public static final int[][] fieldOrganic = new int[FIELD_WIDTH][FIELD_HEIGHT];

	public static final Array<Molecule> molecules = new Array<>();
	Array<Integer> initialGenome = new Array<>();
	int cnt = 0;

	@Override
	public void create () {
		batch = new SpriteBatch();
		camera = new OrthographicCamera();
		camera.setToOrtho(false,SCR_WIDTH,SCR_HEIGHT);

		loadResources();
		fontGenerate();

		shapeRenderer = new ShapeRenderer();
		shapeRenderer.setAutoShapeType(true);

		stage = new Stage();

		createUI();

		for(int i = 0;i < FIELD_WIDTH;i++){
			for(int j = 0;j < FIELD_HEIGHT;j++){
				field[i][j] = -1;
			}
		}

		for(int i = 0;i < FIELD_WIDTH;i++){
			for(int j = 0;j < FIELD_HEIGHT;j++){
				fieldSun[i][j] = j/3;
			}
		}

		for(int i = 0;i < FIELD_WIDTH;i++){
			for(int j = 0;j < FIELD_HEIGHT;j++){
				fieldOrganic[i][j] = 0;
			}
		}

		for(int i = 0;i < GENOME_SIZE;i++){
			initialGenome.add(22);
		}

		for(int i = 0;i < 10;i++){
			int x = MathUtils.random(0,FIELD_WIDTH-1);
			int y = MathUtils.random(0,FIELD_HEIGHT-1);
			while (field[x][y] != -1){
				x = MathUtils.random(0,FIELD_WIDTH-1);
				y = MathUtils.random(0,FIELD_HEIGHT-1);
			}
			molecules.add(new Molecule(x,y,initialGenome));
			field[x][y] = i;
		}
	}

	@Override
	public void render () {
		ScreenUtils.clear(0, 0, 0, 1);
		batch.begin();

		int maxEnergy = 0;
		int maxEnergyIndex = 0;

		int sz = molecules.size;
		for(int i = 0;i < sz;i++){
			molecules.get(i).makeMove();
		}
		++cnt;

		for(int a = 0;a < molecules.size;a++){
			if(!molecules.get(a).isAlive()){
				++fieldOrganic[molecules.get(a).getX()][molecules.get(a).getY()];
				for(int i = a;i < molecules.size;i++){
					if(i == a){
						field[molecules.get(i).getX()][molecules.get(i).getY()] = -1;
					}
					else{
						--field[molecules.get(i).getX()][molecules.get(i).getY()];
					}
				}
				molecules.removeIndex(a);
			}
			else{
				if(molecules.get(a).getEnergy() > maxEnergy){
					maxEnergy = molecules.get(a).getEnergy();
					maxEnergyIndex = a;
				}
			}
		}

		font.draw(batch,"Killed\n" + killed,SCR_WIDTH + 20,215);
		font.draw(batch,"Population\n" + molecules.size,SCR_WIDTH + 20,140);
		font.draw(batch,"Iteration\n" + cnt,SCR_WIDTH + 20,65);
		font.draw(batch,molecules.get(maxEnergyIndex).genome.toString(),0,SCR_HEIGHT + 25);

		if(viewState != BASIC_VIEW){
			batch.end();
		}

		for(int i = 0;i < molecules.size;i++){
			if(viewState == BASIC_VIEW) {
				batch.draw(imgMolecule, molecules.get(i).getX() * CELL_SIZE, molecules.get(i).getY() * CELL_SIZE, CELL_SIZE, CELL_SIZE);
			}
			else if(viewState == ENERGY_VIEW){
				shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
				Color colorMolecule = new Color(1f,1f-(molecules.get(i).getEnergy())/700f,0f,1f);
				shapeRenderer.setColor(colorMolecule);
				shapeRenderer.rect(molecules.get(i).getX() * CELL_SIZE, molecules.get(i).getY() * CELL_SIZE, CELL_SIZE, CELL_SIZE);
				shapeRenderer.end();
			}
			else if(viewState == NUTRITION_VIEW){
				shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
				float cntGreen = 0;
				float cntRed = 0;
				float cntBlue = 0;
				for(int j = 0;j < molecules.get(i).genome.size;j++){
					if(molecules.get(i).genome.get(j) == 22){
						cntGreen += 1f;
					}
					if(molecules.get(i).genome.get(j) == 53){
						cntRed += 1f;
					}
					if(molecules.get(i).genome.get(j) == 59){
						cntBlue += 1f;
					}
				}
				Color colorMolecule = new Color(cntRed/cntGreen,cntGreen/cntRed,cntBlue/cntGreen,1f);
				shapeRenderer.setColor(colorMolecule);
				shapeRenderer.rect(molecules.get(i).getX() * CELL_SIZE, molecules.get(i).getY() * CELL_SIZE, CELL_SIZE, CELL_SIZE);
				shapeRenderer.end();
			}
		}

		try {
			Thread.sleep(50,1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(viewState == BASIC_VIEW){
			batch.end();
		}
		stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
		stage.draw();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
	}

	void loadResources(){
		imgMolecule = new Texture("molecule.png");
	}
	void fontGenerate(){
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("robotomonomedium.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		final String FONT_CHARS = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяabcdefghijklmnopqrstuvwxyzАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789][_!$%#@|\\/?-+=()*&.;:,{}\"´`'<>";
		parameter.characters = FONT_CHARS;
		parameter.size = 20;
		parameter.color = Color.WHITE;
		font = generator.generateFont(parameter);
		parameter.size = 15;
		smallFont = generator.generateFont(parameter);
		generator.dispose();
	}

	void createUI(){
//		Skin skin;
//		TextureAtlas buttonAtlas;
		Gdx.input.setInputProcessor(stage);
//		skin = new Skin();
//		buttonAtlas = new TextureAtlas(Gdx.files.internal("buttons/buttons.pack"));
//		skin.addRegions(buttonAtlas);
		textButtonStyle = new TextButton.TextButtonStyle();
		textButtonStyle.font = smallFont;
//		textButtonStyle.up = skin.getDrawable("up-button");
//		textButtonStyle.down = skin.getDrawable("down-button");
//		textButtonStyle.checked = skin.getDrawable("checked-button");

		basicViewButton = new TextButton("Basic", textButtonStyle);
		basicViewButton.setPosition(SCR_WIDTH + 10,SCR_HEIGHT - 20);
		basicViewButton.addListener(new ClickListener(){
			public void clicked(InputEvent e, float x, float y) {
				viewState = BASIC_VIEW;
			}
		});
		stage.addActor(basicViewButton);

		energyViewButton = new TextButton("Energy", textButtonStyle);
		energyViewButton.setPosition(SCR_WIDTH + 10,SCR_HEIGHT - 50);
		energyViewButton.addListener(new ClickListener(){
			public void clicked(InputEvent e, float x, float y) {
				viewState = ENERGY_VIEW;
			}
		});
		stage.addActor(energyViewButton);

		nutritionViewButton = new TextButton("Nutrition", textButtonStyle);
		nutritionViewButton.setPosition(SCR_WIDTH + 10,SCR_HEIGHT - 80);
		nutritionViewButton.addListener(new ClickListener(){
			public void clicked(InputEvent e, float x, float y) {
				viewState = NUTRITION_VIEW;
			}
		});
		stage.addActor(nutritionViewButton);

		saveButton = new TextButton("Save", textButtonStyle);
		saveButton.setPosition(SCR_WIDTH + 10,SCR_HEIGHT - 130);
		saveButton.addListener(new ClickListener(){
			public void clicked(InputEvent e, float x, float y) {
				saveState();
			}
		});
		stage.addActor(saveButton);

		loadButton = new TextButton("Load", textButtonStyle);
		loadButton.setPosition(SCR_WIDTH + 10,SCR_HEIGHT - 160);
		loadButton.addListener(new ClickListener(){
			public void clicked(InputEvent e, float x, float y) {
				loadFile(loadFileName);
			}
		});
		stage.addActor(loadButton);

		Skin skin = new Skin();
		skin.add("font",smallFont);

		tableContainer = new Container<>();
		tableContainer.setSize(90,500);
		tableContainer.setPosition(SCR_WIDTH + 10,SCR_HEIGHT - 190 - 500);
		tableContainer.left();

		table = new Table(skin);

		try {
			Set<String> files = listFiles(saveFilePath);
			for(final String value : files){
				TextButton textButton = new TextButton(value.substring(0,value.length()-5), textButtonStyle);
				textButton.addListener(new ClickListener(){
					public void clicked(InputEvent e, float x, float y) {
						loadFileName = value;
					}
				});

				table.add(textButton);
				table.row().expandX();

				ScrollPane scrollPane = new ScrollPane(table);
				scrollPane.setScrollingDisabled(true,false);

				tableContainer.setActor(scrollPane);
				stage.addActor(tableContainer);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	void saveState() {
		final String name = Calendar.getInstance().getTime().hashCode() + ".save";
		String path = saveFilePath + name;
		File file = new File(path);
		TextButton textButton = new TextButton(name.substring(0,name.length()-5), textButtonStyle);
		textButton.addListener(new ClickListener(){
			public void clicked(InputEvent e, float x, float y) {
				loadFileName = name;
			}
		});

		table.add(textButton);
		table.row().expandX();

		try {
			if (file.createNewFile()){
				System.err.println(file.getName());

				PrintWriter writer = new PrintWriter(file);

				writer.println(killed);
				writer.println(molecules.size);
				writer.println(cnt);

				for(int i = 0;i < molecules.size;i++){
					writer.println(molecules.get(i).getX() + " " + molecules.get(i).getY());
					for(int j = 0;j < GENOME_SIZE;j++){
						writer.print(molecules.get(i).genome.get(j) + " ");
					}
					writer.println();
				}

				for(int i = 0;i < FIELD_WIDTH;i++){
					for(int j = 0;j < FIELD_HEIGHT;j++){
						writer.print(field[i][j] + " ");
					}
					writer.println();
				}

				for(int i = 0;i < FIELD_WIDTH;i++){
					for(int j = 0;j < FIELD_HEIGHT;j++){
						writer.print(fieldSun[i][j] + " ");
					}
					writer.println();
				}

				for(int i = 0;i < FIELD_WIDTH;i++){
					for(int j = 0;j < FIELD_HEIGHT;j++){
						writer.print(fieldOrganic[i][j] + " ");
					}
					writer.println();
				}

				writer.close();
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	void loadFile(String name){
		try {
			FileInputStream input = new FileInputStream(saveFilePath + name);
			Scanner in = new Scanner(input);

			killed = in.nextInt();
			int n = in.nextInt();
			cnt = in.nextInt();

			molecules.clear();
			for(int i = 0;i < n;i++){
				Array<Integer> array = new Array<>();
				int x = in.nextInt();
				int y = in.nextInt();
				for(int j = 0;j < GENOME_SIZE;j++){
					int a = in.nextInt();
					array.add(a);
				}
				molecules.add(new Molecule(x,y,array));
			}

			for(int i = 0;i < FIELD_WIDTH;i++){
				for(int j = 0;j < FIELD_HEIGHT;j++){
					field[i][j] = in.nextInt();
				}
			}

			for(int i = 0;i < FIELD_WIDTH;i++){
				for(int j = 0;j < FIELD_HEIGHT;j++){
					fieldSun[i][j] = in.nextInt();
				}
			}

			for(int i = 0;i < FIELD_WIDTH;i++){
				for(int j = 0;j < FIELD_HEIGHT;j++){
					fieldOrganic[i][j] = in.nextInt();
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}


	Set<String> listFiles(String dir) throws IOException{
		final Set<String> fileList = new HashSet<>();

		Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if(!Files.isDirectory(file)){
					fileList.add(file.getFileName().toString());
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return fileList;
	}
}