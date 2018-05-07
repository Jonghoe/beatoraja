package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Mode;
import bms.model.NormalNote;
import bms.model.Note;
import bms.model.TimeLine;


public class LaneShuffleModifier extends PatternModifier{
	

	private int[] random;
	
	private int type;
	
	private int[] result;
	
	private int[] keys;
	
	private int lanes;
	private int[] ln;
	private int[] endLnNoteTime;
	private int max = 0;
	private boolean isImpossible = false;
	
	private LaneShuffleModifier lan;
	
	private List<Integer> originalPatternList = new ArrayList<Integer>(); 
	private List<List<Integer>> kouhoPatternList = new ArrayList<List<Integer>>();
	
	
	public static final int MIRROR = 0;
	/**
	 * �꺆�꺖�깇�꺖�깉
	 */
	public static final int R_RANDOM = 1;
	/**
	 * �꺀�꺍���깲
	 */
	public static final int RANDOM = 2;
	/**
	 * �궚�꺆�궧
	 */
	public static final int CROSS = 3;
	/**
	 * �궧�궚�꺀�긿�긽�꺃�꺖�꺍�굮�맜���꺀�꺍���깲
	 */
	public static final int RANDOM_EX = 4;
	/**
	 * 1P-2P�굮�뀯�굦�쎘�걟�굥
	 */
	public static final int FLIP = 5;
	/**
	 * 1P�겗鈺쒒씊�굮2P�겓�궠�깞�꺖�걲�굥
	 */
	public static final int BATTLE = 6;
	
	

	public LaneShuffleModifier(int type) {
		super(type == RANDOM_EX ? 1 : 0);
		this.type = type;
	}
	
	

	public int[] noMurioshiLaneShuffle(BMSModel model) {
		
		//Set value to be used this method
		Mode mode = model.getMode();
		keys = getKeys(mode, false);
		lanes = mode.key;
		ln = new int[lanes];
		endLnNoteTime = new int[lanes];
		
		for (int key : keys) {
			max = Math.max(max, key);
		}
		
		Arrays.fill(ln, -1);
		Arrays.fill(endLnNoteTime, -1);
		
		
		//Initialize All value associated with LN
		Init_Ln(model, lanes, keys, ln, endLnNoteTime, isImpossible, originalPatternList);
		
	
		//Set Flag Value
		Set_murioshiFlag(isImpossible ,originalPatternList,keys, kouhoPatternList);
		
		
		//SetResult
		return Set_Result(result, kouhoPatternList);
	}

	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		Random rand = new Random(model, lan, type);
		random = rand.makeRandom();
		
		List<PatternModifyLog> log = new ArrayList();
		lanes = model.getMode().key;
		TimeLine[] timelines = model.getAllTimeLines();
		for (int index = 0; index < timelines.length; index++) {
			final TimeLine tl = timelines[index];
			if (tl.existNote() || tl.existHiddenNote()) {
				Note[] notes = new Note[lanes];
				Note[] hnotes = new Note[lanes];
				for (int i = 0; i < lanes; i++) {
					notes[i] = tl.getNote(i);
					hnotes[i] = tl.getHiddenNote(i);
				}
				boolean[] clone = new boolean[lanes];
				for (int i = 0; i < lanes; i++) {
					final int mod = i < random.length ? random[i] : i;
					if (clone[mod]) {
						if (notes[mod] != null) {
							if (notes[mod] instanceof LongNote && ((LongNote) notes[mod]).isEnd()) {
								for (int j = index - 1; j >= 0; j--) {
									if (((LongNote) notes[mod]).getPair().getSection() == timelines[j].getSection()) {
										LongNote ln = (LongNote) timelines[j].getNote(i);
										tl.setNote(i, ln.getPair());
										System.out.println(ln.toString() + " : " + ln.getPair().toString() + " == "
												+ ((LongNote) notes[mod]).getPair().toString() + " : "
												+ notes[mod].toString());
										break;
									}
								}
							} else {
								tl.setNote(i, (Note) notes[mod].clone());
							}
						} else {
							tl.setNote(i, null);
						}
						if (hnotes[mod] != null) {
							tl.setHiddenNote(i, (Note) hnotes[mod].clone());
						} else {
							tl.setHiddenNote(i, null);
						}
					} else {
						tl.setNote(i, notes[mod]);
						tl.setHiddenNote(i, hnotes[mod]);
						clone[mod] = true;
					}
				}
				log.add(new PatternModifyLog(tl.getSection(), random));
			}
		}
		return log;
	}
	
	private void Init_Ln(BMSModel model, int lanes, int[] keys, int[] ln, int[] endLnNoteTime,
			boolean isImpossible, List<Integer> originalPatternList) {
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.existNote()) {
				//LN
				Set_LN(tl);
				
				List<Integer> noteLane = new ArrayList<Integer>(keys.length);
				for (int i = 0; i < lanes; i++) {
					Note n = tl.getNote(i);
					if((n != null && n instanceof NormalNote ) || (ln != null && ln[i] != -1)) {
							noteLane.add((Integer) i);
					}
				}
				
				if(noteLane.size() >= 7) {
					isImpossible = true;
					break;
				} else if(noteLane.size() >= 3) {
					int pattern=0;
					for(int i=0;i<noteLane.size();i++) {
						pattern += (int) Math.pow(2, noteLane.get(i));
					}
					originalPatternList.add((Integer) pattern);
				}
			}
		}
	}
	
	private void Set_LN(TimeLine tl) {
		for (int i = 0; i < lanes; i++) {
			Note n = tl.getNote(i);
			if (n instanceof LongNote) {
				LongNote ln2 = (LongNote) n;
				if (ln2.isEnd() && tl.getTime() == endLnNoteTime[i]) {
					ln[i] = -1;
					endLnNoteTime[i] = -1;
				} else {
					ln[i] = i;
					if (!ln2.isEnd()) {
						endLnNoteTime[i] = ln2.getPair().getTime();
					}
				}
			}
		}
	}
	
	private void Set_murioshiFlag(boolean isImpossible ,List<Integer> originalPatternList, int[] keys, 
			List<List<Integer>> kouhoPatternList) {
		if(!isImpossible) {
			
			for(int i = 0 ; i < originalPatternList.size()-1 ; i++ ) {
				for(int j = originalPatternList.size()-1 ; j > i; j-- ) {
					if (originalPatternList.get(i).equals(originalPatternList.get(j))) {
						originalPatternList.remove(j);
					}
				}
			}
			
			int[] searchLane = new int[9];
			boolean[] searchLaneFlag = new boolean[9];
			Arrays.fill(searchLaneFlag, false);
			List<Integer> tempPattern = new ArrayList<Integer>(keys.length);
			for(searchLane[0]=0;searchLane[0]<9;searchLane[0]++) {
				searchLaneFlag[searchLane[0]] = true;
				for(searchLane[1]=0;searchLane[1]<9;searchLane[1]++) {
					if(searchLaneFlag[searchLane[1]]) continue;
					searchLaneFlag[searchLane[1]] = true;
					for(searchLane[2]=0;searchLane[2]<9;searchLane[2]++) {
						if(searchLaneFlag[searchLane[2]]) continue;
						searchLaneFlag[searchLane[2]] = true;
						for(searchLane[3]=0;searchLane[3]<9;searchLane[3]++) {
							if(searchLaneFlag[searchLane[3]]) continue;
							searchLaneFlag[searchLane[3]] = true;
							for(searchLane[4]=0;searchLane[4]<9;searchLane[4]++) {
								if(searchLaneFlag[searchLane[4]]) continue;
								searchLaneFlag[searchLane[4]] = true;
								for(searchLane[5]=0;searchLane[5]<9;searchLane[5]++) {
									if(searchLaneFlag[searchLane[5]]) continue;
									searchLaneFlag[searchLane[5]] = true;
									for(searchLane[6]=0;searchLane[6]<9;searchLane[6]++) {
										if(searchLaneFlag[searchLane[6]]) continue;
										searchLaneFlag[searchLane[6]] = true;
										for(searchLane[7]=0;searchLane[7]<9;searchLane[7]++) {
											if(searchLaneFlag[searchLane[7]]) continue;
											searchLaneFlag[searchLane[7]] = true;
											for(searchLane[8]=0;searchLane[8]<9;searchLane[8]++) {
												
												if(searchLaneFlag[searchLane[8]] || (searchLane[0]==0&&searchLane[1]==1&&searchLane[2]==2&&searchLane[3]==3&&searchLane[4]==4&&searchLane[5]==5&&searchLane[6]==6&&searchLane[7]==7&&searchLane[8]==8)
																				 || (searchLane[0]==8&&searchLane[1]==7&&searchLane[2]==6&&searchLane[3]==5&&searchLane[4]==4&&searchLane[5]==3&&searchLane[6]==2&&searchLane[7]==1&&searchLane[8]==0)) continue; 
												boolean murioshiFlag = false;
												kouhoPatternList.add(new ArrayList<Integer>());
												for(int i=0;i<9;i++) {
													kouhoPatternList.get(kouhoPatternList.size()-1).add(searchLane[i]);
												}
												
												for(int i=0;i<originalPatternList.size();i++) {
													tempPattern.clear();
													for(int j=0;j<9;j++) {
														if(((int)(originalPatternList.get(i)/Math.pow(2,j))%2)==1) tempPattern.add((Integer) searchLane[j]+1);
													}
													if(
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)4)!=-1&&tempPattern.indexOf((Integer)7)!=-1)||
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)4)!=-1&&tempPattern.indexOf((Integer)8)!=-1)||
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)4)!=-1&&tempPattern.indexOf((Integer)9)!=-1)||
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)5)!=-1&&tempPattern.indexOf((Integer)8)!=-1)||
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)5)!=-1&&tempPattern.indexOf((Integer)9)!=-1)||
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)6)!=-1&&tempPattern.indexOf((Integer)9)!=-1)||
															(tempPattern.indexOf((Integer)2)!=-1&&tempPattern.indexOf((Integer)5)!=-1&&tempPattern.indexOf((Integer)8)!=-1)||
															(tempPattern.indexOf((Integer)2)!=-1&&tempPattern.indexOf((Integer)5)!=-1&&tempPattern.indexOf((Integer)9)!=-1)||
															(tempPattern.indexOf((Integer)2)!=-1&&tempPattern.indexOf((Integer)6)!=-1&&tempPattern.indexOf((Integer)9)!=-1)||
															(tempPattern.indexOf((Integer)3)!=-1&&tempPattern.indexOf((Integer)6)!=-1&&tempPattern.indexOf((Integer)9)!=-1)
															) {
														murioshiFlag=true;
														break;
													}
												}
												
											}
											searchLaneFlag[searchLane[7]] = false;
										}
										searchLaneFlag[searchLane[6]] = false;
									}
									searchLaneFlag[searchLane[5]] = false;
								}
								searchLaneFlag[searchLane[4]] = false;
							}
							searchLaneFlag[searchLane[3]] = false;
						}
						searchLaneFlag[searchLane[2]] = false;
					}
					searchLaneFlag[searchLane[1]] = false;
				}
				searchLaneFlag[searchLane[0]] = false;
			}
		}
		
		Logger.getGlobal().info("Murioshi Sheet Number: "+(kouhoPatternList.size()));
	}
	
	private int[] Set_Result(int[] result, List<List<Integer>> kouhoPatternList) {
		
		if(kouhoPatternList.size() > 0) {
			int r = (int) (Math.random() * kouhoPatternList.size());
			for (int i = 0; i < 9; i++) {
				result[(int) (kouhoPatternList.get(r)).get(i)] = i;
			}
		
		} else {
			int mirror = (int) (Math.random() * 2);
			for (int i = 0; i < 9; i++) {
				result[i] = mirror == 0 ? i : 8 - i;
			}
		}
		
		return result;
	}

}
