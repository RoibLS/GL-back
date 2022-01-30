package com.example.demo.service;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.example.demo.exception.DateException;
import com.example.demo.exception.ExpenseBillException;
import com.example.demo.exception.LineBillException;
import com.example.demo.repository.ExpenseBillRepository;
import com.example.demo.vo.Advance;
import com.example.demo.vo.BillStates;
import com.example.demo.vo.ExpenseBill;
import com.example.demo.vo.LineBill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.sound.sampled.Line;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ExpenseBillService {
    @Autowired
    ExpenseBillRepository expenseBillRepository;

    @Autowired
    DateService dateService;

    public ExpenseBill saveExpenseBill(ExpenseBill expenseBill) throws ExpenseBillException, DateException {

        //dateService.isCoherent(expenseBill.getDate());

        if (this.expenseBillRepository.existsById(expenseBill.getId())) {
            throw new ExpenseBillException("Id of the bill already exists", HttpStatus.CONFLICT);
        }

        return this.expenseBillRepository.save(expenseBill);
    }

    public HttpStatus deleteExpenseBill(int id) throws ExpenseBillException {
        //Check existence of this id
        if(!expenseBillRepository.existsById(id)){
            throw new ExpenseBillException("Id doesn't exist", HttpStatus.BAD_REQUEST);
        }
        this.expenseBillRepository.delete(this.expenseBillRepository.findById(id));
        return HttpStatus.OK;
    }

    public void addLineBill(LineBill lineBill, int idExpenseBill) throws ExpenseBillException {
        if(!expenseBillRepository.existsById(idExpenseBill)){
            throw new ExpenseBillException("Id doesn't exist", HttpStatus.BAD_REQUEST);
        }

        ExpenseBill expenseBill = expenseBillRepository.findById(idExpenseBill);
        List<LineBill> list = expenseBill.getListLineBill();
        list.add(lineBill);
        expenseBill.setListLineBill(list);
        expenseBill.setAmount(expenseBill.getAmount()+lineBill.getAmount());
        expenseBillRepository.save(expenseBill);
    }

    public List<ExpenseBill> getExpenseBillList() {
        return StreamSupport.stream(this.expenseBillRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }


    public void existsById(int idExpenseBill) throws ExpenseBillException {
        if(!this.expenseBillRepository.existsById(idExpenseBill)){
            throw new ExpenseBillException("This Expense Bill does not exists",HttpStatus.BAD_REQUEST);
        }
    }

    // TODO check aussi que les avances sont valide
    public boolean allValidated(int expenseBillId) throws ExpenseBillException {
        existsById(expenseBillId);
        ExpenseBill expenseBill = expenseBillRepository.findById(expenseBillId);
        boolean res = true;
        for(LineBill lineBill : expenseBill.getListLineBill()){
            if(!lineBill.isValidated()){
                res = false;
                break;
            }
        }
        return res;
    }

    public void setStateValidated(int expenseBillId){
        ExpenseBill expenseBill = expenseBillRepository.findById(expenseBillId);
        expenseBill.setState(BillStates.VALIDATED);
        expenseBillRepository.save(expenseBill);
    }


    public HttpStatus validExpenseBill(int expenseBillId) throws LineBillException, ExpenseBillException {
        if (!this.expenseBillRepository.existsById(expenseBillId)) {
            throw new ExpenseBillException("Impossible to update an inexisting expenseBill", HttpStatus.CONFLICT);
        }
        ExpenseBill expenseBill = expenseBillRepository.findById(expenseBillId);
        List<LineBill> listLine = expenseBill.getListLineBill();

        for(LineBill lineBill : listLine){
            lineBill.setValidated(true);
        }
        setStateValidated(expenseBillId);
        return(HttpStatus.OK);
    }

    public void checkState(int idExpenseBill) throws ExpenseBillException {
        ExpenseBill expense = this.expenseBillRepository.findById(idExpenseBill);
        if(expense.getState()==BillStates.VALIDATED){
            throw new ExpenseBillException("This expense bill already validated", HttpStatus.CONFLICT);
        }else if (expense.getState()==BillStates.WAITING){
            throw new ExpenseBillException("This expense bill is waiting for validation and so can't be modified", HttpStatus.CONFLICT);
        }

    }

    public float getTotal() {

        float total = 0;
        List<ExpenseBill> list = getExpenseBillList();
        for (ExpenseBill e : list){
            if(e.getState() == BillStates.DRAFT || e.getState() == BillStates.WAITING){
                total+=e.getAmount();
            }
        }
        return total;
    }

    //TODO penser a faire une fonction qui gere le moment ou le colaborateur demande a valider sa note

    public ExpenseBill sendForValidation(int expenseBillId) throws LineBillException, ExpenseBillException {
        if (!this.expenseBillRepository.existsById(expenseBillId)) {
            throw new ExpenseBillException("Impossible to update an inexisting expenseBill", HttpStatus.CONFLICT);
        }
        ExpenseBill expenseBill = expenseBillRepository.findById(expenseBillId);
        expenseBill.setState(BillStates.WAITING);
        expenseBillRepository.save(expenseBill);
        return expenseBill;
    }

    public int getNumberBillsNonValidated() {
        int res = 0;
        List<ExpenseBill> list = getExpenseBillList();
        for (ExpenseBill e : list){
            if(e.getState() == BillStates.DRAFT || e.getState() == BillStates.WAITING){
                res+=1;
            }
        }
        return res;
    }

    public ExpenseBill getExpenseBillById(int id) throws ExpenseBillException {
        if (!this.expenseBillRepository.existsById(id)) {
            throw new ExpenseBillException("this expenseBill does not exists", HttpStatus.CONFLICT);
        }
        return this.expenseBillRepository.findById(id);
    }


    public void verifDate(String date) throws ExpenseBillException {
        List<ExpenseBill> list = getExpenseBillList();
        for(ExpenseBill e : list){
            if(e.getDate().equals(date)){
                throw new ExpenseBillException("A bill already has this date",HttpStatus.CONFLICT);
            }
        }
    }

    public String getMounth(String date){return date.substring(5, 7);}

    public String getYear(String date){return date.substring(0,4);}

    /*public boolean existsByDate(LocalDate date){
        int mounthDate = date.getMonthValue();
        Iterable<ExpenseBill> list = this.expenseBillRepository.findAll();
        for (ExpenseBill expenseBill : list){
            if( getMounth(expenseBill.getDate()) == mounthDate){
                return true;
            }
        }
        return false;
    }*/

    public HttpStatus addAdvanceToCurrentBill(Advance advance) throws ExpenseBillException, DateException {

        List<ExpenseBill> list = getExpenseBillList();
        String currentDate = LocalDate.now().toString();
        String currentYear = getYear(currentDate);
        String currentMounth = getMounth(currentDate);

        String expenseBillDate = currentYear+"-"+currentMounth;

        boolean billExists = false;

        for(ExpenseBill expenseBill : list) {
            if (expenseBill.getDate().equals(expenseBillDate)) {
                List<Advance> listAdvance = expenseBill.getListAdvance();
                listAdvance.add(advance);
                expenseBill.setListAdvance(listAdvance);
                this.expenseBillRepository.save(expenseBill);
                billExists = true;
                break;
            }
        }

        if(!billExists){
            List<Advance> listAdvance = new ArrayList<Advance>();
            listAdvance.add(advance);

            this.expenseBillRepository.save(ExpenseBill.builder()
                    .amount(0)
                    .listLineBill(new ArrayList<>())
                    .listAdvance(listAdvance)
                    .name("")
                    .date(expenseBillDate)
                    .description("")
                    .state(BillStates.DRAFT)
                    .build());
        }


        return HttpStatus.OK;


    }

}
