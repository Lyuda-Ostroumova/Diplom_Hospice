package ru.iteco.fmhandroid.ui.data;

import static ru.iteco.fmhandroid.ui.data.Helper.getCurrentDate;
import static ru.iteco.fmhandroid.ui.data.Helper.getCurrentTime;

import io.bloco.faker.Faker;

public class Resources {

// News
    public String newsPublicationDate = getCurrentDate();
    public String dateForNonExistentNews = "12.09.2045";
    public String newsPublicationTime = getCurrentTime();
    public String newsDescriptionLatin = "News Description";
    public String newsDescriptionCyr = "Описание новости";
    public String newsDescriptionSymbols = "#$%^@#&((*";
    public String newsDescriptionSpace = " ";
    public String newsTitleLatin = "News Title";
    public String newsTitleCyr = "Название новости";
    public String newsTitleSymbols = "#$%^@#&((*";
    public String newsTitleSpace = " ";

    // Claims
    public String claimPublicationDate = getCurrentDate();
    public String dateForNonExistentClaim = "12.09.2045";
    public String claimPublicationTime = getCurrentTime();
    public String claimDescriptionLatin = "Claim Description";
    public String claimDescriptionCyr = "Нет горячей воды на 1 этаже";
    public String claimDescriptionSymbols = "#$%^@#&((*";
    public String claimDescriptionSpace = " ";
    public String claimTitleLatin = "Claim Title";
    public String claimTitle51 = "В этом названии теперь больше пятидесяти символов! Но поместится только 50";
    public String claimTitleCyr = "Водоснабжение";
    public String claimTitleSymbols = "#$%^@#&((*";
    public String claimTitleSpace = " ";
    public String claimEditedTitle = "Новое название";
    public String claimEditedDescription = "Новое описание";



    // Comment
    public String commentLatin = "Comment";
    public String commentCyr = "Комментарий";
    public String commentSpace = " ";
    public String commentSymbols = "#$%^@#&((*";
    public String editedComment = "Отредактированный комментарий";



}
