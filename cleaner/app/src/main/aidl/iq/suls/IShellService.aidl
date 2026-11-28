package iq.suls;

interface IShellService {

    // رقم الدالة محجوز من Shizuku نفسه لإنهاء الخدمة
    void destroy() = 16777114;

    // ينفّذ أمر شل حقيقي بهوية شل/روت (حسب كيف بدأ المستخدم Shizuku)
    String exec(String cmd) = 1;
}
