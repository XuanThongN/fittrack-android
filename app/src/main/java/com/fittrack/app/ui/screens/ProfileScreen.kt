package com.fittrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.app.data.db.FitTrackRepository
import com.fittrack.app.data.model.UserProfile
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.OrangeBurn
import com.fittrack.app.ui.theme.TextMuted
import com.fittrack.app.ui.theme.TextPrimary
import com.fittrack.app.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: FitTrackRepository
) {
    val profile by repository.userProfile.collectAsState()
    val weights by repository.weightEntries.collectAsState()
    val currentWeight = weights.firstOrNull()?.weightKg ?: profile.initialWeightKg

    var name by remember(profile) { mutableStateOf(profile.name) }
    var heightCm by remember(profile) { mutableStateOf(profile.heightCm.toString()) }
    var initialWeight by remember(profile) { mutableStateOf(profile.initialWeightKg.toString()) }
    var targetWeight by remember(profile) { mutableStateOf(profile.targetWeightKg.toString()) }
    var age by remember(profile) { mutableStateOf(profile.age.toString()) }
    var gender by remember(profile) { mutableStateOf(profile.gender) }
    var activityLevel by remember(profile) { mutableStateOf(profile.activityLevel) }

    var isSavedNotification by remember { mutableStateOf(false) }

    val activityOptions = listOf(
        "sedentary" to "Ít vận động (làm việc văn phòng)",
        "light" to "Vận động nhẹ (tập 1-3 ngày/tuần)",
        "moderate" to "Vận động vừa (tập 3-5 ngày/tuần)",
        "active" to "Năng động (tập 6-7 ngày/tuần)",
        "very_active" to "Rất năng động (lao động nặng/thể thao chuyên)"
    )

    var expandedDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hồ Sơ & Thể Trạng",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Chỉ số BMR, TDEE & Mục tiêu cân nặng",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HEALTH METRICS SUMMARY CARD
        val bmr = profile.calculateBmr(currentWeight)
        val tdee = profile.calculateTdee(currentWeight)
        val deficitTarget = profile.getWeightLossTargetCalories(currentWeight)
        val bmi = profile.calculateBmi(currentWeight)
        val bmiCat = profile.getBmiCategory(bmi)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Phân Tích Chỉ Số Trao Đổi Chất",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "BMI Hiện tại", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = "$bmi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(bmiCat.colorHex)
                        )
                        Text(
                            text = bmiCat.label,
                            fontSize = 11.sp,
                            color = Color(bmiCat.colorHex)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "BMR (Tối thiểu)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = "${bmr.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(text = "kcal/ngày", fontSize = 11.sp, color = TextMuted)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "TDEE (Tổng đốt)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = "${tdee.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OrangeBurn
                        )
                        Text(text = "kcal/ngày", fontSize = 11.sp, color = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "💡 Mục tiêu nạp calo mỗi ngày:",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Nạp khoảng ${deficitTarget.toInt()} kcal/ngày (giảm 500 kcal so với TDEE) sẽ giúp bạn giảm an toàn ~0.5kg mỡ mỗi tuần mà không lo mất cơ bắp.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // EDIT PROFILE FORM
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Thông Tin Cá Nhân",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên gọi của bạn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = heightCm,
                        onValueChange = { heightCm = it },
                        label = { Text("Chiều cao (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Tuổi") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = initialWeight,
                        onValueChange = { initialWeight = it },
                        label = { Text("Cân ban đầu (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = targetWeight,
                        onValueChange = { targetWeight = it },
                        label = { Text("Mục tiêu (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // GENDER SELECT
                Column {
                    Text(text = "Giới tính", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = gender.lowercase() == "male",
                                onClick = { gender = "male" },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Text(text = "Nam", color = TextPrimary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = gender.lowercase() == "female",
                                onClick = { gender = "female" },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Text(text = "Nữ", color = TextPrimary)
                        }
                    }
                }

                // ACTIVITY LEVEL SELECT
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    val currentLabel = activityOptions.firstOrNull { it.first == activityLevel }?.second
                        ?: "Vận động vừa (tập 3-5 ngày/tuần)"
                    OutlinedTextField(
                        value = currentLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mức độ hoạt động thể chất") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        activityOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    activityLevel = key
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val h = heightCm.toDoubleOrNull() ?: profile.heightCm
                        val initW = initialWeight.toDoubleOrNull() ?: profile.initialWeightKg
                        val tgtW = targetWeight.toDoubleOrNull() ?: profile.targetWeightKg
                        val a = age.toIntOrNull() ?: profile.age

                        val updated = profile.copy(
                            name = name.ifBlank { "Bạn" },
                            heightCm = h,
                            initialWeightKg = initW,
                            targetWeightKg = tgtW,
                            age = a,
                            gender = gender,
                            activityLevel = activityLevel
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.saveProfile(updated)
                        }
                        isSavedNotification = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = DarkBackground)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Lưu Hồ Sơ", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                if (isSavedNotification) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Đã cập nhật thông tin thành công!", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
