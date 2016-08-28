package com.company.minery.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.company.minery.Constants;
import com.company.minery.utils.AssetResolution;

public class GameAssets implements Disposable {
	
	public class TextureRegionExt extends TextureRegion {
		
		private float width;
		private float height;
		
		public TextureRegionExt(final TextureRegion region) {
			this(region, 1f);
		}

		public TextureRegionExt(final TextureRegion region,
								final float scale) {
			
			super(region);
			
			if(region != null) {
				setSize(region.getRegionWidth() * scale, region.getRegionHeight() * scale);
			}
		}
		
		public TextureRegionExt(final TextureRegionExt region) {
			this(region, 1f);
		} 
		
		public TextureRegionExt(final TextureRegionExt region,
								final float scale) {
			
			super(region);
			
			if(region != null) {
				setSize(region.getWidth() * scale, region.getHeight() * scale);
			}
		}

		public void setSize(final float width,
							final float height) {
			
			this.width = width;
			this.height = height;
		}
		
		public void scale(final float scale) {
			this.width *= scale;
			this.height *= scale;
		}
		
		public void rescale(final float scale) {
			this.width = this.getRegionWidth() * scale;
			this.height = this.getRegionHeight() * scale;
		}
		
		public float getWidth() {
			return width;
		}
		
		public float getHeight() {
			return height;
		}
		
	}
	
	public AssetResolution resolution;
	
	public final TextureAtlas atlas;
	public final Element testMapXml;
	
	public final TextureRegionExt characterBody;
	public final TextureRegionExt characterFist;
	public final TextureRegionExt characterFoot;
	public final TextureRegionExt characterHead;
	public final TextureRegionExt spear;
	public final TextureRegionExt heartBig;
	public final TextureRegionExt heartSmall;
	public final TextureRegionExt defeatLabel;
	public final TextureRegionExt victoryLabel;
	
	public GameAssets() {
		final XmlReader xmlParser = new XmlReader();
		testMapXml = xmlParser.parse(Gdx.files.internal("assets/maps/RuinedCastle.tmx").readString());
		
		atlas = new TextureAtlas("assets/textures/textures.atlas");
		characterBody = new TextureRegionExt(atlas.findRegion("body"), 1);
		characterFist = new TextureRegionExt(atlas.findRegion("fist"), 1);
		characterFoot = new TextureRegionExt(atlas.findRegion("foot"), 1);
		characterHead = new TextureRegionExt(atlas.findRegion("head"), 1);
		spear = new TextureRegionExt(atlas.findRegion("spear"), 1);
		heartBig = new TextureRegionExt(atlas.findRegion("heart_big"), 1);
		heartSmall = new TextureRegionExt(atlas.findRegion("heart_small"), 1);
		defeatLabel = new TextureRegionExt(atlas.findRegion("defeat"), 1);
		victoryLabel = new TextureRegionExt(atlas.findRegion("victory"), 1);
	}
	
	public final void rescale(final AssetResolution resolution) {
		this.resolution = resolution;
		
		characterBody.rescale(Constants.PIXELART_SCALE * resolution.calcScale());
		characterFist.rescale(Constants.PIXELART_SCALE * resolution.calcScale());
		characterFoot.rescale(Constants.PIXELART_SCALE * resolution.calcScale());
		characterHead.rescale(Constants.PIXELART_SCALE * resolution.calcScale());
		spear.rescale(Constants.PIXELART_SCALE * resolution.calcScale());
		heartBig.rescale(Constants.PIXELART_SCALE * resolution.calcScale());
		heartSmall.rescale(Constants.PIXELART_SCALE * resolution.calcScale());
		defeatLabel.rescale(Constants.PIXELART_SCALE * resolution.calcScale());
		victoryLabel.rescale(Constants.PIXELART_SCALE * resolution.calcScale());
	}
	
	@Override
	public void dispose() {
		atlas.dispose();
	}
	
}
