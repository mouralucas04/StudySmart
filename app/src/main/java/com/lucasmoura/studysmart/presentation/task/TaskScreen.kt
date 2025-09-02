package com.lucasmoura.studysmart.presentation.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucasmoura.studysmart.presentation.components.DeleteDialog
import com.lucasmoura.studysmart.presentation.components.SubjectListBottomSheet
import com.lucasmoura.studysmart.presentation.components.TaskCheckBox
import com.lucasmoura.studysmart.presentation.components.TaskDatePicker
import com.lucasmoura.studysmart.ui.theme.StudySmartTheme
import com.lucasmoura.studysmart.util.Priority
import com.lucasmoura.studysmart.util.SnackbarEvent
import com.lucasmoura.studysmart.util.changeMillisToDateString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

data class TaskScreenNavArgs(
    val taskId: Int?,
    val subjectId: Int?
)

@Destination<RootGraph>(navArgs = TaskScreenNavArgs::class)
@Composable
fun TaskScreenRoute(
    navigator: DestinationsNavigator
) {
    val viewModel: TaskViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBarEvent = viewModel.snackBarEventFlow

    TaskScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackBarEvent = snackBarEvent,
        onBackButtonClick = {
            navigator.navigateUp()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskScreen(
    state: TaskState,
    onEvent: (TaskEvent) -> Unit,
    snackBarEvent: SharedFlow<SnackbarEvent>,
    onBackButtonClick: () -> Unit,
) {

    var isDeleteDialogOpen by rememberSaveable {
        mutableStateOf(false)
    }

    var isDatePickerDialogOpen by rememberSaveable {
        mutableStateOf(false)
    }

    val sheetState = rememberModalBottomSheetState()
    var isBottomSheetOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val now = LocalDate.now()
    val dayStart = now.atTime(0, 0, 0, 0)
        .toEpochSecond(ZoneOffset.UTC) * 1000

    val snackBarHostState = remember {
        SnackbarHostState()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dayStart,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= dayStart
            }
        }
    )

    var taskTitleError by rememberSaveable { mutableStateOf<String?>(null) }
    taskTitleError = when {
        state.title.isBlank() -> "Please enter task title"
        state.title.length < 4 -> "Task title is too short"
        state.title.length > 30 -> "Task title is too long"
        else -> null
    }

//    val taskTitleError by remember {
//        derivedStateOf {
//            when {
//                state.title.isBlank() -> "Please enter task title"
//                state.title.length < 4 -> "Task title is too short"
//                state.title.length > 30 -> "Task title is too long"
//                else -> null
//            }
//        }
//    }

    DeleteDialog(
        isOpen = isDeleteDialogOpen,
        title = "Delete Task?",
        bodyText = "Are you sure you want to delete this task? " +
                "This action can not be undone",
        onDismissRequest = {
            isDeleteDialogOpen = false
        },
        onConfirmButton = {
            onEvent(TaskEvent.DeleteTask)
            isDeleteDialogOpen = false
        }
    )

    TaskDatePicker(
        state = datePickerState,
        isOpen = isDatePickerDialogOpen,
        onDismissRequest = { isDatePickerDialogOpen = false },
        onConfirmButtonClick = {
            onEvent(TaskEvent.OnDateChange(millis = datePickerState.selectedDateMillis))
            isDatePickerDialogOpen = false
        },
    )

    LaunchedEffect(key1 = true) {
        snackBarEvent.collectLatest { event ->
            when (event) {
                is SnackbarEvent.ShowSnackbar -> {
                    snackBarHostState.showSnackbar(
                        message = event.message,
                        duration = event.duration
                    )
                }

                SnackbarEvent.NavigateUp -> {
                    onBackButtonClick()
                }
            }
        }
    }

    SubjectListBottomSheet(
        sheetState = sheetState,
        isOpen = isBottomSheetOpen,
        subjects = state.subjects,
        onSubjectClicked = { subject ->
            scope.launch {
                sheetState.hide()
            }.invokeOnCompletion {
                if (!sheetState.isVisible) isBottomSheetOpen = false
            }

            onEvent(TaskEvent.OnRelatedSubjectSelect(subject))
        },
        onDismissRequest = { isBottomSheetOpen = false }
    )



    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        },
        topBar = {
            TaskScreenTopBar(
                isTaskExist = state.currentTaskId != null,
                isComplete = state.isTaskComplete,
                checkBoxBorderColor = state.priority.color,
                onBackButtonClick = onBackButtonClick,
                onDeleteButtonClick = { isDeleteDialogOpen = true },
                onCheckBoxClick = { onEvent(TaskEvent.OnIsCompleteChange) },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.title,
                onValueChange = { onEvent(TaskEvent.OnTitleChange(it)) },
                label = { Text(text = "Title") },
                singleLine = true,
                isError = taskTitleError != null && state.title.isNotBlank(),
                supportingText = { Text(text = taskTitleError.orEmpty()) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.description,
                onValueChange = { onEvent(TaskEvent.OnDescriptionChange(it)) },
                label = { Text(text = "Description") },
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Due Date",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.dueDate.changeMillisToDateString(),
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(
                    onClick = { isDatePickerDialogOpen = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Duo Date"
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Priority",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Priority.entries.forEach { priority ->
                    PriorityButton(
                        modifier = Modifier.weight(1f),
                        label = priority.title,
                        backgroundColor = priority.color,
                        borderColor = if (priority == state.priority) {
                            Color.White
                        } else Color.Transparent,
                        labelColor = if (priority == state.priority) {
                            Color.White
                        } else Color.White.copy(alpha = 0.7f),
                        onClick = { onEvent(TaskEvent.OnPriorityChange(priority)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Related to Subject",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val firstSubject = state.subjects.firstOrNull()?.name ?: ""
                Text(
                    text = state.relatedToSubject ?: firstSubject,
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(
                    onClick = { isBottomSheetOpen = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Subject"
                    )
                }
            }

            Button(
                enabled = taskTitleError == null,
                onClick = { onEvent(TaskEvent.SaveTask) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Text(text = "Save")

            }


        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskScreenTopBar(
    isTaskExist: Boolean,
    isComplete: Boolean,
    checkBoxBorderColor: Color,
    onBackButtonClick: () -> Unit,
    onDeleteButtonClick: () -> Unit,
    onCheckBoxClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onBackButtonClick
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Navigate Back"
                )
            }
        },
        title = { Text(text = "Task", style = MaterialTheme.typography.headlineSmall) },
        actions = {
            if (isTaskExist) {
                TaskCheckBox(
                    isComplete = isComplete,
                    borderColor = checkBoxBorderColor,
                    onCheckBoxClick = onCheckBoxClick,
                )
                IconButton(
                    onClick = onDeleteButtonClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task"
                    )
                }
            }
        }
    )
}

/* Que a mensagem de Cristo, com toda a sua riqueza, viva no coração de vocês. Ensinem
e instruam uns aos outros com toda a sabedoria. Cantem salmos, hinos e canções espirituais;
louvem a Deus, com gratidão no coração. E tudo o que vocês fizerem ou disserem, façam em
nome do Senhor Jesus e por meio dele agradeçam a Deus, o Pai. Colossences 3:16
*
*
*
* */

@Composable
private fun PriorityButton(
    modifier: Modifier = Modifier,
    label: String,
    backgroundColor: Color,
    borderColor: Color,
    labelColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(5.dp)
            .border(1.dp, borderColor, RoundedCornerShape(5.dp))
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = labelColor)
    }

}

@Preview
@Composable
private fun TaskScreenPreview() {
    StudySmartTheme {
        TaskScreen(
            onBackButtonClick = { },
            state = TaskState(),
            onEvent = { },
            snackBarEvent = MutableSharedFlow()
        )
    }

}