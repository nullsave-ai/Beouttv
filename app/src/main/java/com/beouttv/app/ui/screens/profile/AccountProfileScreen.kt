package com.beouttv.app.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beouttv.app.data.ghost.UserAccountManager
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.ui.components.BeoutIconButton
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles

data class AvatarCharacter(
    val id: Int,
    val icon: ImageVector,
    val titleAr: String,
    val titleEn: String
)

val BEOUT_AVATARS = listOf(
    AvatarCharacter(8, Icons.Rounded.Face, "شخصية الأنمي", "Anime Star"),
    AvatarCharacter(1, Icons.Rounded.Person, "المشاهد المتميز", "VIP Viewer"),
    AvatarCharacter(2, Icons.Rounded.SportsSoccer, "الرياضي / الكروي", "Sports Star"),
    AvatarCharacter(3, Icons.Rounded.Movie, "المخرج السينمائي", "Cinema Director"),
    AvatarCharacter(4, Icons.Rounded.RocketLaunch, "رائد الفضاء", "Astronaut"),
    AvatarCharacter(5, Icons.Rounded.Stars, "النجم الذهبي", "Golden Star"),
    AvatarCharacter(6, Icons.Rounded.Security, "الشخصية الأسطورية", "Legend Star"),
    AvatarCharacter(7, Icons.Rounded.Search, "المحقق الذكي", "Detective"),
    AvatarCharacter(9, Icons.Rounded.Explore, "المستكشف الشجاع", "Voyager"),
    AvatarCharacter(10, Icons.Rounded.WorkspacePremium, "التاج الملكي", "Royal Crown"),
    AvatarCharacter(11, Icons.Rounded.Headphones, "خبير الصوتيات", "Audio Master"),
    AvatarCharacter(12, Icons.Rounded.Tv, "نجم البث المباشر", "Live Master")
)

@Composable
fun AccountProfileScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentProfile by UserAccountManager.profileState.collectAsState()

    var username by remember { mutableStateOf(currentProfile.username) }
    var selectedAvatarId by remember { mutableIntStateOf(currentProfile.avatarId) }

    LaunchedEffect(Unit) {
        val loaded = UserAccountManager.loadProfile(context)
        username = loaded.username
        selectedAvatarId = loaded.avatarId

        // Prompt for notification permission politely when visiting account if not yet granted
        val notifManager = androidx.core.app.NotificationManagerCompat.from(context)
        if (!notifManager.areNotificationsEnabled()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                (context as? android.app.Activity)?.requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeoutIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = S.back(),
                onClick = onBack
            )
            Spacer(Modifier.width(BeoutDimensions.SpaceS))
            Text(
                text = tr("الملف الشخصي والحساب", "Account & Profile", "Hesap ve Profil", "حساب و نمایه", "Compte et profil"),
                style = BeoutTextStyles.ScreenTitle,
                color = BeoutColors.TextPrimary
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceS),
            verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceM),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Current Avatar & Profile Hero Card
            item {
                val currentAvatar = BEOUT_AVATARS.firstOrNull { it.id == selectedAvatarId } ?: BEOUT_AVATARS.first()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(BeoutShapes.Large)
                        .background(
                            Brush.verticalGradient(
                                listOf(BeoutColors.ElevatedSurface, BeoutColors.Card)
                            )
                        )
                        .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.4f), BeoutShapes.Large)
                        .padding(vertical = BeoutDimensions.SpaceM, horizontal = BeoutDimensions.SpaceL)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Large Avatar with Glowing Ring
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(BeoutColors.BrightBlue.copy(alpha = 0.15f))
                                .border(2.5.dp, Brush.horizontalGradient(listOf(BeoutColors.BrightBlue, BeoutColors.PrimaryBlue)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = currentAvatar.icon,
                                contentDescription = null,
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(Modifier.height(BeoutDimensions.SpaceS))

                        Text(
                            text = username.ifBlank { tr("مستخدم بي اوت", "Beout User", "Beout Kullanıcısı", "کاربر بی اوت", "Utilisateur Beout") },
                            style = BeoutTextStyles.SectionTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.TextPrimary
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = tr(currentAvatar.titleAr, currentAvatar.titleEn, currentAvatar.titleEn, currentAvatar.titleAr, currentAvatar.titleEn),
                            style = BeoutTextStyles.SecondaryInfo.copy(color = BeoutColors.BrightBlue, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        )

                        Spacer(Modifier.height(BeoutDimensions.SpaceS))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(BeoutShapes.Small)
                                    .background(BeoutColors.Surface)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tr("عضو 2026", "Member 2026", "2026 Üye", "عضو ۲۰۲۶", "Membre 2026"),
                                    style = BeoutTextStyles.Caption.copy(color = BeoutColors.TextSecondary)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(BeoutShapes.Small)
                                    .background(BeoutColors.BrightBlue.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tr("مشفر محلياً 100%", "100% Encrypted Local", "%100 Şifreli Yerel", "۱۰۰٪ رمزگذاری محلی", "100% Chiffré local"),
                                    style = BeoutTextStyles.Caption.copy(color = BeoutColors.BrightBlue, fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Name Customization with Dice / Random Button
            item {
                Column {
                    Text(
                        text = tr("اسم المستخدم:", "Display Username:", "Kullanıcı Adı:", "نام کاربری:", "Nom d'utilisateur :"),
                        style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp),
                        color = BeoutColors.TextPrimary
                    )
                    Spacer(Modifier.height(BeoutDimensions.SpaceXS))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { if (it.length <= 25) username = it },
                            placeholder = { Text(tr("اكتب اسمك أو لقبك هنا...", "Enter your name...", "Adınızı girin...", "نام خود را وارد کنید...", "Entrez votre nom...")) },
                            singleLine = true,
                            shape = BeoutShapes.Medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BeoutColors.BrightBlue,
                                unfocusedBorderColor = BeoutColors.Divider,
                                focusedTextColor = BeoutColors.TextPrimary,
                                unfocusedTextColor = BeoutColors.TextPrimary,
                                focusedContainerColor = BeoutColors.ElevatedSurface,
                                unfocusedContainerColor = BeoutColors.ElevatedSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Dice / Shuffle Button
                        OutlinedButton(
                            onClick = {
                                username = UserAccountManager.getRandomName()
                            },
                            shape = BeoutShapes.Medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = BeoutColors.ElevatedSurface,
                                contentColor = BeoutColors.BrightBlue
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(BeoutColors.BrightBlue, BeoutColors.PrimaryBlue))
                            ),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Casino,
                                contentDescription = "اسم عشوائي",
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 3. Compact Horizontal Avatar Carousel
            item {
                Column {
                    Text(
                        text = tr("اختر الشخصية الرمزية (الآفاتار):", "Choose Avatar Character:", "Karakter Avatarını Seçin:", "انتخاب آواتار:", "Choisir l'avatar :"),
                        style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp),
                        color = BeoutColors.TextPrimary
                    )
                    Spacer(Modifier.height(BeoutDimensions.SpaceS))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(BEOUT_AVATARS) { avatar ->
                            val isSelected = selectedAvatarId == avatar.id
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) BeoutColors.BrightBlue else Color.Transparent,
                                label = "avatarBorder"
                            )
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.22f) else BeoutColors.ElevatedSurface,
                                label = "avatarBg"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(76.dp)
                                    .clip(BeoutShapes.Medium)
                                    .background(bgColor)
                                    .border(1.5.dp, borderColor, BeoutShapes.Medium)
                                    .clickable { selectedAvatarId = avatar.id }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.3f) else BeoutColors.Surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = avatar.icon,
                                        contentDescription = avatar.titleAr,
                                        tint = if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = tr(avatar.titleAr, avatar.titleEn, avatar.titleEn, avatar.titleAr, avatar.titleEn),
                                    style = BeoutTextStyles.Caption.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 4. Save Button
            item {
                Spacer(Modifier.height(BeoutDimensions.SpaceS))
                Button(
                    onClick = {
                        UserAccountManager.saveProfile(context, username, selectedAvatarId)
                        Toast.makeText(
                            context,
                            tr("تم حفظ بيانات الملف الشخصي بنجاح", "Profile saved successfully", "Profil başarıyla kaydedildi", "نمایه با موفقیت ذخیره شد", "Profil enregistré"),
                            Toast.LENGTH_SHORT
                        ).show()
                        onBack()
                    },
                    shape = BeoutShapes.Medium,
                    colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.PrimaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tr("حفظ التعديلات", "Save Changes", "Değişiklikleri Kaydet", "ذخیره تغییرات", "Enregistrer les modifications"),
                        style = BeoutTextStyles.Button.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}
