/*   1:    */ package nxt.crypto;
/*   2:    */ 
/*   3:    */ final class Curve25519
/*   4:    */ {
/*   5:    */   public static final int KEY_SIZE = 32;
/*   6: 18 */   public static final byte[] ZERO = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
/*   7: 24 */   public static final byte[] PRIME = { -19, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 127 };
/*   8: 36 */   public static final byte[] ORDER = { -19, -45, -11, 92, 26, 99, 18, 88, -42, -100, -9, -94, -34, -7, -34, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16 };
/*   9:    */   private static final int P25 = 33554431;
/*  10:    */   private static final int P26 = 67108863;
/*  11:    */   
/*  12:    */   public static void clamp(byte[] paramArrayOfByte)
/*  13:    */   {
/*  14: 54 */     paramArrayOfByte[31] = ((byte)(paramArrayOfByte[31] & 0x7F));
/*  15: 55 */     paramArrayOfByte[31] = ((byte)(paramArrayOfByte[31] | 0x40)); int 
/*  16: 56 */       tmp22_21 = 0;paramArrayOfByte[tmp22_21] = ((byte)(paramArrayOfByte[tmp22_21] & 0xF8));
/*  17:    */   }
/*  18:    */   
/*  19:    */   public static void keygen(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3)
/*  20:    */   {
/*  21: 68 */     clamp(paramArrayOfByte3);
/*  22: 69 */     core(paramArrayOfByte1, paramArrayOfByte2, paramArrayOfByte3, null);
/*  23:    */   }
/*  24:    */   
/*  25:    */   public static void curve(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3)
/*  26:    */   {
/*  27: 78 */     core(paramArrayOfByte1, null, paramArrayOfByte2, paramArrayOfByte3);
/*  28:    */   }
/*  29:    */   
/*  30:    */   public static boolean sign(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
/*  31:    */   {
/*  32:127 */     byte[] arrayOfByte1 = new byte[65];
/*  33:128 */     byte[] arrayOfByte2 = new byte[33];
/*  34:131 */     for (int j = 0; j < 32; j++) {
/*  35:132 */       paramArrayOfByte1[j] = 0;
/*  36:    */     }
/*  37:133 */     j = mula_small(paramArrayOfByte1, paramArrayOfByte3, 0, paramArrayOfByte2, 32, -1);
/*  38:134 */     mula_small(paramArrayOfByte1, paramArrayOfByte1, 0, ORDER, 32, (15 - paramArrayOfByte1[31]) / 16);
/*  39:135 */     mula32(arrayOfByte1, paramArrayOfByte1, paramArrayOfByte4, 32, 1);
/*  40:136 */     divmod(arrayOfByte2, arrayOfByte1, 64, ORDER, 32);
/*  41:137 */     int i = 0;
/*  42:137 */     for (j = 0; j < 32; j++) {
/*  43:138 */       i |= (paramArrayOfByte1[j] = arrayOfByte1[j]);
/*  44:    */     }
/*  45:139 */     return i != 0;
/*  46:    */   }
/*  47:    */   
/*  48:    */   public static void verify(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
/*  49:    */   {
/*  50:150 */     byte[] arrayOfByte = new byte[32];
/*  51:    */     
/*  52:152 */     long10[] arrayOflong101 = { new long10(), new long10() };
/*  53:153 */     long10[] arrayOflong102 = { new long10(), new long10() };
/*  54:154 */     long10[] arrayOflong103 = { new long10(), new long10(), new long10() };
/*  55:155 */     long10[] arrayOflong104 = { new long10(), new long10(), new long10() };
/*  56:156 */     long10[] arrayOflong105 = { new long10(), new long10(), new long10() };
/*  57:157 */     long10[] arrayOflong106 = { new long10(), new long10(), new long10() };
/*  58:    */     
/*  59:159 */     int i = 0;int j = 0;int k = 0;int m = 0;
/*  60:    */     
/*  61:    */ 
/*  62:    */ 
/*  63:163 */     set(arrayOflong101[0], 9);
/*  64:164 */     unpack(arrayOflong101[1], paramArrayOfByte4);
/*  65:    */     
/*  66:    */ 
/*  67:    */ 
/*  68:    */ 
/*  69:    */ 
/*  70:    */ 
/*  71:171 */     x_to_y2(arrayOflong105[0], arrayOflong106[0], arrayOflong101[1]);
/*  72:172 */     sqrt(arrayOflong105[0], arrayOflong106[0]);
/*  73:173 */     int i1 = is_negative(arrayOflong105[0]);
/*  74:174 */     arrayOflong106[0]._0 += 39420360L;
/*  75:175 */     mul(arrayOflong106[1], BASE_2Y, arrayOflong105[0]);
/*  76:176 */     sub(arrayOflong105[i1], arrayOflong106[0], arrayOflong106[1]);
/*  77:177 */     add(arrayOflong105[(1 - i1)], arrayOflong106[0], arrayOflong106[1]);
/*  78:178 */     cpy(arrayOflong106[0], arrayOflong101[1]);
/*  79:179 */     arrayOflong106[0]._0 -= 9L;
/*  80:180 */     sqr(arrayOflong106[1], arrayOflong106[0]);
/*  81:181 */     recip(arrayOflong106[0], arrayOflong106[1], 0);
/*  82:182 */     mul(arrayOflong102[0], arrayOflong105[0], arrayOflong106[0]);
/*  83:183 */     sub(arrayOflong102[0], arrayOflong102[0], arrayOflong101[1]);
/*  84:184 */     arrayOflong102[0]._0 -= 486671L;
/*  85:185 */     mul(arrayOflong102[1], arrayOflong105[1], arrayOflong106[0]);
/*  86:186 */     sub(arrayOflong102[1], arrayOflong102[1], arrayOflong101[1]);
/*  87:187 */     arrayOflong102[1]._0 -= 486671L;
/*  88:188 */     mul_small(arrayOflong102[0], arrayOflong102[0], 1L);
/*  89:189 */     mul_small(arrayOflong102[1], arrayOflong102[1], 1L);
/*  90:193 */     for (int n = 0; n < 32; n++)
/*  91:    */     {
/*  92:194 */       i = i >> 8 ^ paramArrayOfByte2[n] & 0xFF ^ (paramArrayOfByte2[n] & 0xFF) << 1;
/*  93:195 */       j = j >> 8 ^ paramArrayOfByte3[n] & 0xFF ^ (paramArrayOfByte3[n] & 0xFF) << 1;
/*  94:196 */       m = i ^ j ^ 0xFFFFFFFF;
/*  95:197 */       k = m & (k & 0x80) >> 7 ^ i;
/*  96:198 */       k ^= m & (k & 0x1) << 1;
/*  97:199 */       k ^= m & (k & 0x2) << 1;
/*  98:200 */       k ^= m & (k & 0x4) << 1;
/*  99:201 */       k ^= m & (k & 0x8) << 1;
/* 100:202 */       k ^= m & (k & 0x10) << 1;
/* 101:203 */       k ^= m & (k & 0x20) << 1;
/* 102:204 */       k ^= m & (k & 0x40) << 1;
/* 103:205 */       arrayOfByte[n] = ((byte)k);
/* 104:    */     }
/* 105:208 */     k = (m & (k & 0x80) << 1 ^ i) >> 8;
/* 106:    */     
/* 107:    */ 
/* 108:211 */     set(arrayOflong103[0], 1);
/* 109:212 */     cpy(arrayOflong103[1], arrayOflong101[k]);
/* 110:213 */     cpy(arrayOflong103[2], arrayOflong102[0]);
/* 111:214 */     set(arrayOflong104[0], 0);
/* 112:215 */     set(arrayOflong104[1], 1);
/* 113:216 */     set(arrayOflong104[2], 1);
/* 114:    */     
/* 115:    */ 
/* 116:    */ 
/* 117:    */ 
/* 118:    */ 
/* 119:    */ 
/* 120:    */ 
/* 121:224 */     i = 0;
/* 122:225 */     j = 0;
/* 123:228 */     for (n = 32; n-- != 0;)
/* 124:    */     {
/* 125:229 */       i = i << 8 | paramArrayOfByte2[n] & 0xFF;
/* 126:230 */       j = j << 8 | paramArrayOfByte3[n] & 0xFF;
/* 127:231 */       k = k << 8 | arrayOfByte[n] & 0xFF;
/* 128:233 */       for (i1 = 8; i1-- != 0;)
/* 129:    */       {
/* 130:234 */         mont_prep(arrayOflong105[0], arrayOflong106[0], arrayOflong103[0], arrayOflong104[0]);
/* 131:235 */         mont_prep(arrayOflong105[1], arrayOflong106[1], arrayOflong103[1], arrayOflong104[1]);
/* 132:236 */         mont_prep(arrayOflong105[2], arrayOflong106[2], arrayOflong103[2], arrayOflong104[2]);
/* 133:    */         
/* 134:238 */         i2 = ((i ^ i >> 1) >> i1 & 0x1) + ((j ^ j >> 1) >> i1 & 0x1);
/* 135:    */         
/* 136:240 */         mont_dbl(arrayOflong103[2], arrayOflong104[2], arrayOflong105[i2], arrayOflong106[i2], arrayOflong103[0], arrayOflong104[0]);
/* 137:    */         
/* 138:242 */         i2 = k >> i1 & 0x2 ^ (k >> i1 & 0x1) << 1;
/* 139:243 */         mont_add(arrayOflong105[1], arrayOflong106[1], arrayOflong105[i2], arrayOflong106[i2], arrayOflong103[1], arrayOflong104[1], arrayOflong101[(k >> i1 & 0x1)]);
/* 140:    */         
/* 141:    */ 
/* 142:246 */         mont_add(arrayOflong105[2], arrayOflong106[2], arrayOflong105[0], arrayOflong106[0], arrayOflong103[2], arrayOflong104[2], arrayOflong102[(((i ^ j) >> i1 & 0x2) >> 1)]);
/* 143:    */       }
/* 144:    */     }
/* 145:251 */     int i2 = (i & 0x1) + (j & 0x1);
/* 146:252 */     recip(arrayOflong105[0], arrayOflong104[i2], 0);
/* 147:253 */     mul(arrayOflong105[1], arrayOflong103[i2], arrayOflong105[0]);
/* 148:    */     
/* 149:255 */     pack(arrayOflong105[1], paramArrayOfByte1);
/* 150:    */   }
/* 151:    */   
/* 152:    */   private static final class long10
/* 153:    */   {
/* 154:    */     public long _0;
/* 155:    */     public long _1;
/* 156:    */     public long _2;
/* 157:    */     public long _3;
/* 158:    */     public long _4;
/* 159:    */     public long _5;
/* 160:    */     public long _6;
/* 161:    */     public long _7;
/* 162:    */     public long _8;
/* 163:    */     public long _9;
/* 164:    */     
/* 165:    */     public long10() {}
/* 166:    */     
/* 167:    */     public long10(long paramLong1, long paramLong2, long paramLong3, long paramLong4, long paramLong5, long paramLong6, long paramLong7, long paramLong8, long paramLong9, long paramLong10)
/* 168:    */     {
/* 169:268 */       this._0 = paramLong1;this._1 = paramLong2;this._2 = paramLong3;
/* 170:269 */       this._3 = paramLong4;this._4 = paramLong5;this._5 = paramLong6;
/* 171:270 */       this._6 = paramLong7;this._7 = paramLong8;this._8 = paramLong9;
/* 172:271 */       this._9 = paramLong10;
/* 173:    */     }
/* 174:    */   }
/* 175:    */   
/* 176:    */   private static void cpy32(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
/* 177:    */   {
/* 178:280 */     for (int i = 0; i < 32; i++) {
/* 179:281 */       paramArrayOfByte1[i] = paramArrayOfByte2[i];
/* 180:    */     }
/* 181:    */   }
/* 182:    */   
/* 183:    */   private static int mula_small(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt1, byte[] paramArrayOfByte3, int paramInt2, int paramInt3)
/* 184:    */   {
/* 185:288 */     int i = 0;
/* 186:289 */     for (int j = 0; j < paramInt2; j++)
/* 187:    */     {
/* 188:290 */       i += (paramArrayOfByte2[(j + paramInt1)] & 0xFF) + paramInt3 * (paramArrayOfByte3[j] & 0xFF);
/* 189:291 */       paramArrayOfByte1[(j + paramInt1)] = ((byte)i);
/* 190:292 */       i >>= 8;
/* 191:    */     }
/* 192:294 */     return i;
/* 193:    */   }
/* 194:    */   
/* 195:    */   private static int mula32(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, int paramInt1, int paramInt2)
/* 196:    */   {
/* 197:302 */     int i = 0;
/* 198:303 */     for (int j = 0; j < paramInt1; j++)
/* 199:    */     {
/* 200:305 */       int k = paramInt2 * (paramArrayOfByte3[j] & 0xFF);
/* 201:306 */       i += mula_small(paramArrayOfByte1, paramArrayOfByte1, j, paramArrayOfByte2, 31, k) + (paramArrayOfByte1[(j + 31)] & 0xFF) + k * (paramArrayOfByte2[31] & 0xFF);
/* 202:    */       
/* 203:308 */       paramArrayOfByte1[(j + 31)] = ((byte)i);
/* 204:309 */       i >>= 8;
/* 205:    */     }
/* 206:311 */     paramArrayOfByte1[(j + 31)] = ((byte)(i + (paramArrayOfByte1[(j + 31)] & 0xFF)));
/* 207:312 */     return i >> 8;
/* 208:    */   }
/* 209:    */   
/* 210:    */   private static void divmod(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt1, byte[] paramArrayOfByte3, int paramInt2)
/* 211:    */   {
/* 212:321 */     int i = 0;
/* 213:322 */     int j = (paramArrayOfByte3[(paramInt2 - 1)] & 0xFF) << 8;
/* 214:323 */     if (paramInt2 > 1) {
/* 215:324 */       j |= paramArrayOfByte3[(paramInt2 - 2)] & 0xFF;
/* 216:    */     }
/* 217:326 */     while (paramInt1-- >= paramInt2)
/* 218:    */     {
/* 219:327 */       int k = i << 16 | (paramArrayOfByte2[paramInt1] & 0xFF) << 8;
/* 220:328 */       if (paramInt1 > 0) {
/* 221:329 */         k |= paramArrayOfByte2[(paramInt1 - 1)] & 0xFF;
/* 222:    */       }
/* 223:331 */       k /= j;
/* 224:332 */       i += mula_small(paramArrayOfByte2, paramArrayOfByte2, paramInt1 - paramInt2 + 1, paramArrayOfByte3, paramInt2, -k);
/* 225:333 */       paramArrayOfByte1[(paramInt1 - paramInt2 + 1)] = ((byte)(k + i & 0xFF));
/* 226:334 */       mula_small(paramArrayOfByte2, paramArrayOfByte2, paramInt1 - paramInt2 + 1, paramArrayOfByte3, paramInt2, -i);
/* 227:335 */       i = paramArrayOfByte2[paramInt1] & 0xFF;
/* 228:336 */       paramArrayOfByte2[paramInt1] = 0;
/* 229:    */     }
/* 230:338 */     paramArrayOfByte2[(paramInt2 - 1)] = ((byte)i);
/* 231:    */   }
/* 232:    */   
/* 233:    */   private static int numsize(byte[] paramArrayOfByte, int paramInt)
/* 234:    */   {
/* 235:342 */     while ((paramInt-- != 0) && (paramArrayOfByte[paramInt] == 0)) {}
/* 236:344 */     return paramInt + 1;
/* 237:    */   }
/* 238:    */   
/* 239:    */   private static byte[] egcd32(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
/* 240:    */   {
/* 241:353 */     int j = 32;
/* 242:354 */     for (int m = 0; m < 32; m++)
/* 243:    */     {
/* 244:355 */       int tmp21_20 = 0;paramArrayOfByte2[m] = tmp21_20;paramArrayOfByte1[m] = tmp21_20;
/* 245:    */     }
/* 246:356 */     paramArrayOfByte1[0] = 1;
/* 247:357 */     int i = numsize(paramArrayOfByte3, 32);
/* 248:358 */     if (i == 0) {
/* 249:359 */       return paramArrayOfByte2;
/* 250:    */     }
/* 251:360 */     byte[] arrayOfByte = new byte[32];
/* 252:    */     for (;;)
/* 253:    */     {
/* 254:362 */       int k = j - i + 1;
/* 255:363 */       divmod(arrayOfByte, paramArrayOfByte4, j, paramArrayOfByte3, i);
/* 256:364 */       j = numsize(paramArrayOfByte4, j);
/* 257:365 */       if (j == 0) {
/* 258:366 */         return paramArrayOfByte1;
/* 259:    */       }
/* 260:367 */       mula32(paramArrayOfByte2, paramArrayOfByte1, arrayOfByte, k, -1);
/* 261:    */       
/* 262:369 */       k = i - j + 1;
/* 263:370 */       divmod(arrayOfByte, paramArrayOfByte3, i, paramArrayOfByte4, j);
/* 264:371 */       i = numsize(paramArrayOfByte3, i);
/* 265:372 */       if (i == 0) {
/* 266:373 */         return paramArrayOfByte2;
/* 267:    */       }
/* 268:374 */       mula32(paramArrayOfByte1, paramArrayOfByte2, arrayOfByte, k, -1);
/* 269:    */     }
/* 270:    */   }
/* 271:    */   
/* 272:    */   private static void unpack(long10 paramlong10, byte[] paramArrayOfByte)
/* 273:    */   {
/* 274:385 */     paramlong10._0 = (paramArrayOfByte[0] & 0xFF | (paramArrayOfByte[1] & 0xFF) << 8 | (paramArrayOfByte[2] & 0xFF) << 16 | (paramArrayOfByte[3] & 0xFF & 0x3) << 24);
/* 275:    */     
/* 276:387 */     paramlong10._1 = ((paramArrayOfByte[3] & 0xFF & 0xFFFFFFFC) >> 2 | (paramArrayOfByte[4] & 0xFF) << 6 | (paramArrayOfByte[5] & 0xFF) << 14 | (paramArrayOfByte[6] & 0xFF & 0x7) << 22);
/* 277:    */     
/* 278:389 */     paramlong10._2 = ((paramArrayOfByte[6] & 0xFF & 0xFFFFFFF8) >> 3 | (paramArrayOfByte[7] & 0xFF) << 5 | (paramArrayOfByte[8] & 0xFF) << 13 | (paramArrayOfByte[9] & 0xFF & 0x1F) << 21);
/* 279:    */     
/* 280:391 */     paramlong10._3 = ((paramArrayOfByte[9] & 0xFF & 0xFFFFFFE0) >> 5 | (paramArrayOfByte[10] & 0xFF) << 3 | (paramArrayOfByte[11] & 0xFF) << 11 | (paramArrayOfByte[12] & 0xFF & 0x3F) << 19);
/* 281:    */     
/* 282:393 */     paramlong10._4 = ((paramArrayOfByte[12] & 0xFF & 0xFFFFFFC0) >> 6 | (paramArrayOfByte[13] & 0xFF) << 2 | (paramArrayOfByte[14] & 0xFF) << 10 | (paramArrayOfByte[15] & 0xFF) << 18);
/* 283:    */     
/* 284:395 */     paramlong10._5 = (paramArrayOfByte[16] & 0xFF | (paramArrayOfByte[17] & 0xFF) << 8 | (paramArrayOfByte[18] & 0xFF) << 16 | (paramArrayOfByte[19] & 0xFF & 0x1) << 24);
/* 285:    */     
/* 286:397 */     paramlong10._6 = ((paramArrayOfByte[19] & 0xFF & 0xFFFFFFFE) >> 1 | (paramArrayOfByte[20] & 0xFF) << 7 | (paramArrayOfByte[21] & 0xFF) << 15 | (paramArrayOfByte[22] & 0xFF & 0x7) << 23);
/* 287:    */     
/* 288:399 */     paramlong10._7 = ((paramArrayOfByte[22] & 0xFF & 0xFFFFFFF8) >> 3 | (paramArrayOfByte[23] & 0xFF) << 5 | (paramArrayOfByte[24] & 0xFF) << 13 | (paramArrayOfByte[25] & 0xFF & 0xF) << 21);
/* 289:    */     
/* 290:401 */     paramlong10._8 = ((paramArrayOfByte[25] & 0xFF & 0xFFFFFFF0) >> 4 | (paramArrayOfByte[26] & 0xFF) << 4 | (paramArrayOfByte[27] & 0xFF) << 12 | (paramArrayOfByte[28] & 0xFF & 0x3F) << 20);
/* 291:    */     
/* 292:403 */     paramlong10._9 = ((paramArrayOfByte[28] & 0xFF & 0xFFFFFFC0) >> 6 | (paramArrayOfByte[29] & 0xFF) << 2 | (paramArrayOfByte[30] & 0xFF) << 10 | (paramArrayOfByte[31] & 0xFF) << 18);
/* 293:    */   }
/* 294:    */   
/* 295:    */   private static boolean is_overflow(long10 paramlong10)
/* 296:    */   {
/* 297:409 */     return ((paramlong10._0 > 67108844L) && ((paramlong10._1 & paramlong10._3 & paramlong10._5 & paramlong10._7 & paramlong10._9) == 33554431L) && ((paramlong10._2 & paramlong10._4 & paramlong10._6 & paramlong10._8) == 67108863L)) || (paramlong10._9 > 33554431L);
/* 298:    */   }
/* 299:    */   
/* 300:    */   private static void pack(long10 paramlong10, byte[] paramArrayOfByte)
/* 301:    */   {
/* 302:422 */     int i = 0;int j = 0;
/* 303:    */     
/* 304:424 */     i = (is_overflow(paramlong10) ? 1 : 0) - (paramlong10._9 < 0L ? 1 : 0);
/* 305:425 */     j = i * -33554432;
/* 306:426 */     i *= 19;
/* 307:427 */     long l = i + paramlong10._0 + (paramlong10._1 << 26);
/* 308:428 */     paramArrayOfByte[0] = ((byte)(int)l);
/* 309:429 */     paramArrayOfByte[1] = ((byte)(int)(l >> 8));
/* 310:430 */     paramArrayOfByte[2] = ((byte)(int)(l >> 16));
/* 311:431 */     paramArrayOfByte[3] = ((byte)(int)(l >> 24));
/* 312:432 */     l = (l >> 32) + (paramlong10._2 << 19);
/* 313:433 */     paramArrayOfByte[4] = ((byte)(int)l);
/* 314:434 */     paramArrayOfByte[5] = ((byte)(int)(l >> 8));
/* 315:435 */     paramArrayOfByte[6] = ((byte)(int)(l >> 16));
/* 316:436 */     paramArrayOfByte[7] = ((byte)(int)(l >> 24));
/* 317:437 */     l = (l >> 32) + (paramlong10._3 << 13);
/* 318:438 */     paramArrayOfByte[8] = ((byte)(int)l);
/* 319:439 */     paramArrayOfByte[9] = ((byte)(int)(l >> 8));
/* 320:440 */     paramArrayOfByte[10] = ((byte)(int)(l >> 16));
/* 321:441 */     paramArrayOfByte[11] = ((byte)(int)(l >> 24));
/* 322:442 */     l = (l >> 32) + (paramlong10._4 << 6);
/* 323:443 */     paramArrayOfByte[12] = ((byte)(int)l);
/* 324:444 */     paramArrayOfByte[13] = ((byte)(int)(l >> 8));
/* 325:445 */     paramArrayOfByte[14] = ((byte)(int)(l >> 16));
/* 326:446 */     paramArrayOfByte[15] = ((byte)(int)(l >> 24));
/* 327:447 */     l = (l >> 32) + paramlong10._5 + (paramlong10._6 << 25);
/* 328:448 */     paramArrayOfByte[16] = ((byte)(int)l);
/* 329:449 */     paramArrayOfByte[17] = ((byte)(int)(l >> 8));
/* 330:450 */     paramArrayOfByte[18] = ((byte)(int)(l >> 16));
/* 331:451 */     paramArrayOfByte[19] = ((byte)(int)(l >> 24));
/* 332:452 */     l = (l >> 32) + (paramlong10._7 << 19);
/* 333:453 */     paramArrayOfByte[20] = ((byte)(int)l);
/* 334:454 */     paramArrayOfByte[21] = ((byte)(int)(l >> 8));
/* 335:455 */     paramArrayOfByte[22] = ((byte)(int)(l >> 16));
/* 336:456 */     paramArrayOfByte[23] = ((byte)(int)(l >> 24));
/* 337:457 */     l = (l >> 32) + (paramlong10._8 << 12);
/* 338:458 */     paramArrayOfByte[24] = ((byte)(int)l);
/* 339:459 */     paramArrayOfByte[25] = ((byte)(int)(l >> 8));
/* 340:460 */     paramArrayOfByte[26] = ((byte)(int)(l >> 16));
/* 341:461 */     paramArrayOfByte[27] = ((byte)(int)(l >> 24));
/* 342:462 */     l = (l >> 32) + (paramlong10._9 + j << 6);
/* 343:463 */     paramArrayOfByte[28] = ((byte)(int)l);
/* 344:464 */     paramArrayOfByte[29] = ((byte)(int)(l >> 8));
/* 345:465 */     paramArrayOfByte[30] = ((byte)(int)(l >> 16));
/* 346:466 */     paramArrayOfByte[31] = ((byte)(int)(l >> 24));
/* 347:    */   }
/* 348:    */   
/* 349:    */   private static void cpy(long10 paramlong101, long10 paramlong102)
/* 350:    */   {
/* 351:471 */     paramlong101._0 = paramlong102._0;paramlong101._1 = paramlong102._1;
/* 352:472 */     paramlong101._2 = paramlong102._2;paramlong101._3 = paramlong102._3;
/* 353:473 */     paramlong101._4 = paramlong102._4;paramlong101._5 = paramlong102._5;
/* 354:474 */     paramlong101._6 = paramlong102._6;paramlong101._7 = paramlong102._7;
/* 355:475 */     paramlong101._8 = paramlong102._8;paramlong101._9 = paramlong102._9;
/* 356:    */   }
/* 357:    */   
/* 358:    */   private static void set(long10 paramlong10, int paramInt)
/* 359:    */   {
/* 360:480 */     paramlong10._0 = paramInt;paramlong10._1 = 0L;
/* 361:481 */     paramlong10._2 = 0L;paramlong10._3 = 0L;
/* 362:482 */     paramlong10._4 = 0L;paramlong10._5 = 0L;
/* 363:483 */     paramlong10._6 = 0L;paramlong10._7 = 0L;
/* 364:484 */     paramlong10._8 = 0L;paramlong10._9 = 0L;
/* 365:    */   }
/* 366:    */   
/* 367:    */   private static void add(long10 paramlong101, long10 paramlong102, long10 paramlong103)
/* 368:    */   {
/* 369:491 */     paramlong102._0 += paramlong103._0;paramlong102._1 += paramlong103._1;
/* 370:492 */     paramlong102._2 += paramlong103._2;paramlong102._3 += paramlong103._3;
/* 371:493 */     paramlong102._4 += paramlong103._4;paramlong102._5 += paramlong103._5;
/* 372:494 */     paramlong102._6 += paramlong103._6;paramlong102._7 += paramlong103._7;
/* 373:495 */     paramlong102._8 += paramlong103._8;paramlong102._9 += paramlong103._9;
/* 374:    */   }
/* 375:    */   
/* 376:    */   private static void sub(long10 paramlong101, long10 paramlong102, long10 paramlong103)
/* 377:    */   {
/* 378:498 */     paramlong102._0 -= paramlong103._0;paramlong102._1 -= paramlong103._1;
/* 379:499 */     paramlong102._2 -= paramlong103._2;paramlong102._3 -= paramlong103._3;
/* 380:500 */     paramlong102._4 -= paramlong103._4;paramlong102._5 -= paramlong103._5;
/* 381:501 */     paramlong102._6 -= paramlong103._6;paramlong102._7 -= paramlong103._7;
/* 382:502 */     paramlong102._8 -= paramlong103._8;paramlong102._9 -= paramlong103._9;
/* 383:    */   }
/* 384:    */   
/* 385:    */   private static long10 mul_small(long10 paramlong101, long10 paramlong102, long paramLong)
/* 386:    */   {
/* 387:510 */     long l = paramlong102._8 * paramLong;
/* 388:511 */     paramlong101._8 = (l & 0x3FFFFFF);
/* 389:512 */     l = (l >> 26) + paramlong102._9 * paramLong;
/* 390:513 */     paramlong101._9 = (l & 0x1FFFFFF);
/* 391:514 */     l = 19L * (l >> 25) + paramlong102._0 * paramLong;
/* 392:515 */     paramlong101._0 = (l & 0x3FFFFFF);
/* 393:516 */     l = (l >> 26) + paramlong102._1 * paramLong;
/* 394:517 */     paramlong101._1 = (l & 0x1FFFFFF);
/* 395:518 */     l = (l >> 25) + paramlong102._2 * paramLong;
/* 396:519 */     paramlong101._2 = (l & 0x3FFFFFF);
/* 397:520 */     l = (l >> 26) + paramlong102._3 * paramLong;
/* 398:521 */     paramlong101._3 = (l & 0x1FFFFFF);
/* 399:522 */     l = (l >> 25) + paramlong102._4 * paramLong;
/* 400:523 */     paramlong101._4 = (l & 0x3FFFFFF);
/* 401:524 */     l = (l >> 26) + paramlong102._5 * paramLong;
/* 402:525 */     paramlong101._5 = (l & 0x1FFFFFF);
/* 403:526 */     l = (l >> 25) + paramlong102._6 * paramLong;
/* 404:527 */     paramlong101._6 = (l & 0x3FFFFFF);
/* 405:528 */     l = (l >> 26) + paramlong102._7 * paramLong;
/* 406:529 */     paramlong101._7 = (l & 0x1FFFFFF);
/* 407:530 */     l = (l >> 25) + paramlong101._8;
/* 408:531 */     paramlong101._8 = (l & 0x3FFFFFF);
/* 409:532 */     paramlong101._9 += (l >> 26);
/* 410:533 */     return paramlong101;
/* 411:    */   }
/* 412:    */   
/* 413:    */   private static long10 mul(long10 paramlong101, long10 paramlong102, long10 paramlong103)
/* 414:    */   {
/* 415:544 */     long l1 = paramlong102._0;long l2 = paramlong102._1;long l3 = paramlong102._2;long l4 = paramlong102._3;long l5 = paramlong102._4;
/* 416:545 */     long l6 = paramlong102._5;long l7 = paramlong102._6;long l8 = paramlong102._7;long l9 = paramlong102._8;long l10 = paramlong102._9;
/* 417:    */     
/* 418:547 */     long l11 = paramlong103._0;long l12 = paramlong103._1;long l13 = paramlong103._2;long l14 = paramlong103._3;long l15 = paramlong103._4;
/* 419:548 */     long l16 = paramlong103._5;long l17 = paramlong103._6;long l18 = paramlong103._7;long l19 = paramlong103._8;long l20 = paramlong103._9;
/* 420:    */     
/* 421:550 */     long l21 = l1 * l19 + l3 * l17 + l5 * l15 + l7 * l13 + l9 * l11 + 2L * (l2 * l18 + l4 * l16 + l6 * l14 + l8 * l12) + 38L * (l10 * l20);
/* 422:    */     
/* 423:    */ 
/* 424:    */ 
/* 425:554 */     paramlong101._8 = (l21 & 0x3FFFFFF);
/* 426:555 */     l21 = (l21 >> 26) + l1 * l20 + l2 * l19 + l3 * l18 + l4 * l17 + l5 * l16 + l6 * l15 + l7 * l14 + l8 * l13 + l9 * l12 + l10 * l11;
/* 427:    */     
/* 428:    */ 
/* 429:    */ 
/* 430:559 */     paramlong101._9 = (l21 & 0x1FFFFFF);
/* 431:560 */     l21 = l1 * l11 + 19L * ((l21 >> 25) + l3 * l19 + l5 * l17 + l7 * l15 + l9 * l13) + 38L * (l2 * l20 + l4 * l18 + l6 * l16 + l8 * l14 + l10 * l12);
/* 432:    */     
/* 433:    */ 
/* 434:    */ 
/* 435:564 */     paramlong101._0 = (l21 & 0x3FFFFFF);
/* 436:565 */     l21 = (l21 >> 26) + l1 * l12 + l2 * l11 + 19L * (l3 * l20 + l4 * l19 + l5 * l18 + l6 * l17 + l7 * l16 + l8 * l15 + l9 * l14 + l10 * l13);
/* 437:    */     
/* 438:    */ 
/* 439:    */ 
/* 440:569 */     paramlong101._1 = (l21 & 0x1FFFFFF);
/* 441:570 */     l21 = (l21 >> 25) + l1 * l13 + l3 * l11 + 19L * (l5 * l19 + l7 * l17 + l9 * l15) + 2L * (l2 * l12) + 38L * (l4 * l20 + l6 * l18 + l8 * l16 + l10 * l14);
/* 442:    */     
/* 443:    */ 
/* 444:    */ 
/* 445:574 */     paramlong101._2 = (l21 & 0x3FFFFFF);
/* 446:575 */     l21 = (l21 >> 26) + l1 * l14 + l2 * l13 + l3 * l12 + l4 * l11 + 19L * (l5 * l20 + l6 * l19 + l7 * l18 + l8 * l17 + l9 * l16 + l10 * l15);
/* 447:    */     
/* 448:    */ 
/* 449:    */ 
/* 450:579 */     paramlong101._3 = (l21 & 0x1FFFFFF);
/* 451:580 */     l21 = (l21 >> 25) + l1 * l15 + l3 * l13 + l5 * l11 + 19L * (l7 * l19 + l9 * l17) + 2L * (l2 * l14 + l4 * l12) + 38L * (l6 * l20 + l8 * l18 + l10 * l16);
/* 452:    */     
/* 453:    */ 
/* 454:    */ 
/* 455:584 */     paramlong101._4 = (l21 & 0x3FFFFFF);
/* 456:585 */     l21 = (l21 >> 26) + l1 * l16 + l2 * l15 + l3 * l14 + l4 * l13 + l5 * l12 + l6 * l11 + 19L * (l7 * l20 + l8 * l19 + l9 * l18 + l10 * l17);
/* 457:    */     
/* 458:    */ 
/* 459:    */ 
/* 460:589 */     paramlong101._5 = (l21 & 0x1FFFFFF);
/* 461:590 */     l21 = (l21 >> 25) + l1 * l17 + l3 * l15 + l5 * l13 + l7 * l11 + 19L * (l9 * l19) + 2L * (l2 * l16 + l4 * l14 + l6 * l12) + 38L * (l8 * l20 + l10 * l18);
/* 462:    */     
/* 463:    */ 
/* 464:    */ 
/* 465:594 */     paramlong101._6 = (l21 & 0x3FFFFFF);
/* 466:595 */     l21 = (l21 >> 26) + l1 * l18 + l2 * l17 + l3 * l16 + l4 * l15 + l5 * l14 + l6 * l13 + l7 * l12 + l8 * l11 + 19L * (l9 * l20 + l10 * l19);
/* 467:    */     
/* 468:    */ 
/* 469:    */ 
/* 470:599 */     paramlong101._7 = (l21 & 0x1FFFFFF);
/* 471:600 */     l21 = (l21 >> 25) + paramlong101._8;
/* 472:601 */     paramlong101._8 = (l21 & 0x3FFFFFF);
/* 473:602 */     paramlong101._9 += (l21 >> 26);
/* 474:603 */     return paramlong101;
/* 475:    */   }
/* 476:    */   
/* 477:    */   private static long10 sqr(long10 paramlong101, long10 paramlong102)
/* 478:    */   {
/* 479:609 */     long l1 = paramlong102._0;long l2 = paramlong102._1;long l3 = paramlong102._2;long l4 = paramlong102._3;long l5 = paramlong102._4;
/* 480:610 */     long l6 = paramlong102._5;long l7 = paramlong102._6;long l8 = paramlong102._7;long l9 = paramlong102._8;long l10 = paramlong102._9;
/* 481:    */     
/* 482:612 */     long l11 = l5 * l5 + 2L * (l1 * l9 + l3 * l7) + 38L * (l10 * l10) + 4L * (l2 * l8 + l4 * l6);
/* 483:    */     
/* 484:614 */     paramlong101._8 = (l11 & 0x3FFFFFF);
/* 485:615 */     l11 = (l11 >> 26) + 2L * (l1 * l10 + l2 * l9 + l3 * l8 + l4 * l7 + l5 * l6);
/* 486:    */     
/* 487:617 */     paramlong101._9 = (l11 & 0x1FFFFFF);
/* 488:618 */     l11 = 19L * (l11 >> 25) + l1 * l1 + 38L * (l3 * l9 + l5 * l7 + l6 * l6) + 76L * (l2 * l10 + l4 * l8);
/* 489:    */     
/* 490:    */ 
/* 491:621 */     paramlong101._0 = (l11 & 0x3FFFFFF);
/* 492:622 */     l11 = (l11 >> 26) + 2L * (l1 * l2) + 38L * (l3 * l10 + l4 * l9 + l5 * l8 + l6 * l7);
/* 493:    */     
/* 494:624 */     paramlong101._1 = (l11 & 0x1FFFFFF);
/* 495:625 */     l11 = (l11 >> 25) + 19L * (l7 * l7) + 2L * (l1 * l3 + l2 * l2) + 38L * (l5 * l9) + 76L * (l4 * l10 + l6 * l8);
/* 496:    */     
/* 497:    */ 
/* 498:628 */     paramlong101._2 = (l11 & 0x3FFFFFF);
/* 499:629 */     l11 = (l11 >> 26) + 2L * (l1 * l4 + l2 * l3) + 38L * (l5 * l10 + l6 * l9 + l7 * l8);
/* 500:    */     
/* 501:631 */     paramlong101._3 = (l11 & 0x1FFFFFF);
/* 502:632 */     l11 = (l11 >> 25) + l3 * l3 + 2L * (l1 * l5) + 38L * (l7 * l9 + l8 * l8) + 4L * (l2 * l4) + 76L * (l6 * l10);
/* 503:    */     
/* 504:    */ 
/* 505:635 */     paramlong101._4 = (l11 & 0x3FFFFFF);
/* 506:636 */     l11 = (l11 >> 26) + 2L * (l1 * l6 + l2 * l5 + l3 * l4) + 38L * (l7 * l10 + l8 * l9);
/* 507:    */     
/* 508:638 */     paramlong101._5 = (l11 & 0x1FFFFFF);
/* 509:639 */     l11 = (l11 >> 25) + 19L * (l9 * l9) + 2L * (l1 * l7 + l3 * l5 + l4 * l4) + 4L * (l2 * l6) + 76L * (l8 * l10);
/* 510:    */     
/* 511:    */ 
/* 512:642 */     paramlong101._6 = (l11 & 0x3FFFFFF);
/* 513:643 */     l11 = (l11 >> 26) + 2L * (l1 * l8 + l2 * l7 + l3 * l6 + l4 * l5) + 38L * (l9 * l10);
/* 514:    */     
/* 515:645 */     paramlong101._7 = (l11 & 0x1FFFFFF);
/* 516:646 */     l11 = (l11 >> 25) + paramlong101._8;
/* 517:647 */     paramlong101._8 = (l11 & 0x3FFFFFF);
/* 518:648 */     paramlong101._9 += (l11 >> 26);
/* 519:649 */     return paramlong101;
/* 520:    */   }
/* 521:    */   
/* 522:    */   private static void recip(long10 paramlong101, long10 paramlong102, int paramInt)
/* 523:    */   {
/* 524:657 */     long10 locallong101 = new long10();
/* 525:658 */     long10 locallong102 = new long10();
/* 526:659 */     long10 locallong103 = new long10();
/* 527:660 */     long10 locallong104 = new long10();
/* 528:661 */     long10 locallong105 = new long10();
/* 529:    */     
/* 530:    */ 
/* 531:664 */     sqr(locallong102, paramlong102);
/* 532:665 */     sqr(locallong103, locallong102);
/* 533:666 */     sqr(locallong101, locallong103);
/* 534:667 */     mul(locallong103, locallong101, paramlong102);
/* 535:668 */     mul(locallong101, locallong103, locallong102);
/* 536:669 */     sqr(locallong102, locallong101);
/* 537:670 */     mul(locallong104, locallong102, locallong103);
/* 538:    */     
/* 539:672 */     sqr(locallong102, locallong104);
/* 540:673 */     sqr(locallong103, locallong102);
/* 541:674 */     sqr(locallong102, locallong103);
/* 542:675 */     sqr(locallong103, locallong102);
/* 543:676 */     sqr(locallong102, locallong103);
/* 544:677 */     mul(locallong103, locallong102, locallong104);
/* 545:678 */     sqr(locallong102, locallong103);
/* 546:679 */     sqr(locallong104, locallong102);
/* 547:680 */     for (int i = 1; i < 5; i++)
/* 548:    */     {
/* 549:681 */       sqr(locallong102, locallong104);
/* 550:682 */       sqr(locallong104, locallong102);
/* 551:    */     }
/* 552:684 */     mul(locallong102, locallong104, locallong103);
/* 553:685 */     sqr(locallong104, locallong102);
/* 554:686 */     sqr(locallong105, locallong104);
/* 555:687 */     for (i = 1; i < 10; i++)
/* 556:    */     {
/* 557:688 */       sqr(locallong104, locallong105);
/* 558:689 */       sqr(locallong105, locallong104);
/* 559:    */     }
/* 560:691 */     mul(locallong104, locallong105, locallong102);
/* 561:692 */     for (i = 0; i < 5; i++)
/* 562:    */     {
/* 563:693 */       sqr(locallong102, locallong104);
/* 564:694 */       sqr(locallong104, locallong102);
/* 565:    */     }
/* 566:696 */     mul(locallong102, locallong104, locallong103);
/* 567:697 */     sqr(locallong103, locallong102);
/* 568:698 */     sqr(locallong104, locallong103);
/* 569:699 */     for (i = 1; i < 25; i++)
/* 570:    */     {
/* 571:700 */       sqr(locallong103, locallong104);
/* 572:701 */       sqr(locallong104, locallong103);
/* 573:    */     }
/* 574:703 */     mul(locallong103, locallong104, locallong102);
/* 575:704 */     sqr(locallong104, locallong103);
/* 576:705 */     sqr(locallong105, locallong104);
/* 577:706 */     for (i = 1; i < 50; i++)
/* 578:    */     {
/* 579:707 */       sqr(locallong104, locallong105);
/* 580:708 */       sqr(locallong105, locallong104);
/* 581:    */     }
/* 582:710 */     mul(locallong104, locallong105, locallong103);
/* 583:711 */     for (i = 0; i < 25; i++)
/* 584:    */     {
/* 585:712 */       sqr(locallong105, locallong104);
/* 586:713 */       sqr(locallong104, locallong105);
/* 587:    */     }
/* 588:715 */     mul(locallong103, locallong104, locallong102);
/* 589:716 */     sqr(locallong102, locallong103);
/* 590:717 */     sqr(locallong103, locallong102);
/* 591:718 */     if (paramInt != 0)
/* 592:    */     {
/* 593:719 */       mul(paramlong101, paramlong102, locallong103);
/* 594:    */     }
/* 595:    */     else
/* 596:    */     {
/* 597:721 */       sqr(locallong102, locallong103);
/* 598:722 */       sqr(locallong103, locallong102);
/* 599:723 */       sqr(locallong102, locallong103);
/* 600:724 */       mul(paramlong101, locallong102, locallong101);
/* 601:    */     }
/* 602:    */   }
/* 603:    */   
/* 604:    */   private static int is_negative(long10 paramlong10)
/* 605:    */   {
/* 606:730 */     return (int)(((is_overflow(paramlong10)) || (paramlong10._9 < 0L) ? 1 : 0) ^ paramlong10._0 & 1L);
/* 607:    */   }
/* 608:    */   
/* 609:    */   private static void sqrt(long10 paramlong101, long10 paramlong102)
/* 610:    */   {
/* 611:735 */     long10 locallong101 = new long10();long10 locallong102 = new long10();long10 locallong103 = new long10();
/* 612:736 */     add(locallong102, paramlong102, paramlong102);
/* 613:737 */     recip(locallong101, locallong102, 1);
/* 614:738 */     sqr(paramlong101, locallong101);
/* 615:739 */     mul(locallong103, locallong102, paramlong101);
/* 616:740 */     locallong103._0 -= 1L;
/* 617:741 */     mul(locallong102, locallong101, locallong103);
/* 618:742 */     mul(paramlong101, paramlong102, locallong102);
/* 619:    */   }
/* 620:    */   
/* 621:    */   private static void mont_prep(long10 paramlong101, long10 paramlong102, long10 paramlong103, long10 paramlong104)
/* 622:    */   {
/* 623:752 */     add(paramlong101, paramlong103, paramlong104);
/* 624:753 */     sub(paramlong102, paramlong103, paramlong104);
/* 625:    */   }
/* 626:    */   
/* 627:    */   private static void mont_add(long10 paramlong101, long10 paramlong102, long10 paramlong103, long10 paramlong104, long10 paramlong105, long10 paramlong106, long10 paramlong107)
/* 628:    */   {
/* 629:763 */     mul(paramlong105, paramlong102, paramlong103);
/* 630:764 */     mul(paramlong106, paramlong101, paramlong104);
/* 631:765 */     add(paramlong101, paramlong105, paramlong106);
/* 632:766 */     sub(paramlong102, paramlong105, paramlong106);
/* 633:767 */     sqr(paramlong105, paramlong101);
/* 634:768 */     sqr(paramlong101, paramlong102);
/* 635:769 */     mul(paramlong106, paramlong101, paramlong107);
/* 636:    */   }
/* 637:    */   
/* 638:    */   private static void mont_dbl(long10 paramlong101, long10 paramlong102, long10 paramlong103, long10 paramlong104, long10 paramlong105, long10 paramlong106)
/* 639:    */   {
/* 640:777 */     sqr(paramlong101, paramlong103);
/* 641:778 */     sqr(paramlong102, paramlong104);
/* 642:779 */     mul(paramlong105, paramlong101, paramlong102);
/* 643:780 */     sub(paramlong102, paramlong101, paramlong102);
/* 644:781 */     mul_small(paramlong106, paramlong102, 121665L);
/* 645:782 */     add(paramlong101, paramlong101, paramlong106);
/* 646:783 */     mul(paramlong106, paramlong101, paramlong102);
/* 647:    */   }
/* 648:    */   
/* 649:    */   private static void x_to_y2(long10 paramlong101, long10 paramlong102, long10 paramlong103)
/* 650:    */   {
/* 651:789 */     sqr(paramlong101, paramlong103);
/* 652:790 */     mul_small(paramlong102, paramlong103, 486662L);
/* 653:791 */     add(paramlong101, paramlong101, paramlong102);
/* 654:792 */     paramlong101._0 += 1L;
/* 655:793 */     mul(paramlong102, paramlong101, paramlong103);
/* 656:    */   }
/* 657:    */   
/* 658:    */   private static void core(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
/* 659:    */   {
/* 660:799 */     long10 locallong101 = new long10();
/* 661:800 */     long10 locallong102 = new long10();
/* 662:801 */     long10 locallong103 = new long10();
/* 663:802 */     long10 locallong104 = new long10();
/* 664:803 */     long10 locallong105 = new long10();
/* 665:    */     
/* 666:805 */     long10[] arrayOflong101 = { new long10(), new long10() };
/* 667:806 */     long10[] arrayOflong102 = { new long10(), new long10() };
/* 668:810 */     if (paramArrayOfByte4 != null) {
/* 669:811 */       unpack(locallong101, paramArrayOfByte4);
/* 670:    */     } else {
/* 671:813 */       set(locallong101, 9);
/* 672:    */     }
/* 673:816 */     set(arrayOflong101[0], 1);
/* 674:817 */     set(arrayOflong102[0], 0);
/* 675:    */     
/* 676:    */ 
/* 677:820 */     cpy(arrayOflong101[1], locallong101);
/* 678:821 */     set(arrayOflong102[1], 1);
/* 679:823 */     for (int i = 32; i-- != 0;)
/* 680:    */     {
/* 681:824 */       if (i == 0) {
/* 682:825 */         i = 0;
/* 683:    */       }
/* 684:827 */       for (j = 8; j-- != 0;)
/* 685:    */       {
/* 686:829 */         int k = (paramArrayOfByte3[i] & 0xFF) >> j & 0x1;
/* 687:830 */         int m = (paramArrayOfByte3[i] & 0xFF ^ 0xFFFFFFFF) >> j & 0x1;
/* 688:831 */         localObject = arrayOflong101[m];
/* 689:832 */         long10 locallong106 = arrayOflong102[m];
/* 690:833 */         long10 locallong107 = arrayOflong101[k];
/* 691:834 */         long10 locallong108 = arrayOflong102[k];
/* 692:    */         
/* 693:    */ 
/* 694:    */ 
/* 695:838 */         mont_prep(locallong102, locallong103, (long10)localObject, locallong106);
/* 696:839 */         mont_prep(locallong104, locallong105, locallong107, locallong108);
/* 697:840 */         mont_add(locallong102, locallong103, locallong104, locallong105, (long10)localObject, locallong106, locallong101);
/* 698:841 */         mont_dbl(locallong102, locallong103, locallong104, locallong105, locallong107, locallong108);
/* 699:    */       }
/* 700:    */     }
/* 701:    */     int j;
/* 702:    */     Object localObject;
/* 703:845 */     recip(locallong102, arrayOflong102[0], 0);
/* 704:846 */     mul(locallong101, arrayOflong101[0], locallong102);
/* 705:847 */     pack(locallong101, paramArrayOfByte1);
/* 706:850 */     if (paramArrayOfByte2 != null)
/* 707:    */     {
/* 708:851 */       x_to_y2(locallong103, locallong102, locallong101);
/* 709:852 */       recip(locallong104, arrayOflong102[1], 0);
/* 710:853 */       mul(locallong103, arrayOflong101[1], locallong104);
/* 711:854 */       add(locallong103, locallong103, locallong101);
/* 712:855 */       locallong103._0 += 486671L;
/* 713:856 */       locallong101._0 -= 9L;
/* 714:857 */       sqr(locallong104, locallong101);
/* 715:858 */       mul(locallong101, locallong103, locallong104);
/* 716:859 */       sub(locallong101, locallong101, locallong102);
/* 717:860 */       locallong101._0 -= 39420360L;
/* 718:861 */       mul(locallong102, locallong101, BASE_R2Y);
/* 719:862 */       if (is_negative(locallong102) != 0) {
/* 720:863 */         cpy32(paramArrayOfByte2, paramArrayOfByte3);
/* 721:    */       } else {
/* 722:865 */         mula_small(paramArrayOfByte2, ORDER_TIMES_8, 0, paramArrayOfByte3, 32, -1);
/* 723:    */       }
/* 724:872 */       byte[] arrayOfByte1 = new byte[32];
/* 725:873 */       byte[] arrayOfByte2 = new byte[64];
/* 726:874 */       localObject = new byte[64];
/* 727:875 */       cpy32(arrayOfByte1, ORDER);
/* 728:876 */       cpy32(paramArrayOfByte2, egcd32(arrayOfByte2, (byte[])localObject, paramArrayOfByte2, arrayOfByte1));
/* 729:877 */       if ((paramArrayOfByte2[31] & 0x80) != 0) {
/* 730:878 */         mula_small(paramArrayOfByte2, paramArrayOfByte2, 0, ORDER, 32, 1);
/* 731:    */       }
/* 732:    */     }
/* 733:    */   }
/* 734:    */   
/* 735:883 */   private static final byte[] ORDER_TIMES_8 = { 104, -97, -82, -25, -46, 24, -109, -64, -78, -26, -68, 23, -11, -50, -9, -90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -128 };
/* 736:895 */   private static final long10 BASE_2Y = new long10(39999547L, 18689728L, 59995525L, 1648697L, 57546132L, 24010086L, 19059592L, 5425144L, 63499247L, 16420658L);
/* 737:899 */   private static final long10 BASE_R2Y = new long10(5744L, 8160848L, 4790893L, 13779497L, 35730846L, 12541209L, 49101323L, 30047407L, 40071253L, 6226132L);
/* 738:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.crypto.Curve25519
 * JD-Core Version:    0.7.0.1
 */