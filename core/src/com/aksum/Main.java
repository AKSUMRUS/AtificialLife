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
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

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

		createButtons();

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

	void createButtons(){
		TextButton.TextButtonStyle textButtonStyle;
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
		basicViewButton.setPosition(SCR_WIDTH + 10,SCR_HEIGHT - 50);
		basicViewButton.addListener(new ClickListener(){
			public void clicked(InputEvent e, float x, float y) {
				viewState = BASIC_VIEW;
			}
		});
		stage.addActor(basicViewButton);

		energyViewButton = new TextButton("Energy", textButtonStyle);
		energyViewButton.setPosition(SCR_WIDTH + 10,SCR_HEIGHT - 80);
		energyViewButton.addListener(new ClickListener(){
			public void clicked(InputEvent e, float x, float y) {
				viewState = ENERGY_VIEW;
			}
		});
		stage.addActor(energyViewButton);

		nutritionViewButton = new TextButton("Nutrition", textButtonStyle);
		nutritionViewButton.setPosition(SCR_WIDTH + 10,SCR_HEIGHT - 110);
		nutritionViewButton.addListener(new ClickListener(){
			public void clicked(InputEvent e, float x, float y) {
				viewState = NUTRITION_VIEW;
			}
		});
		stage.addActor(nutritionViewButton);
	}
}