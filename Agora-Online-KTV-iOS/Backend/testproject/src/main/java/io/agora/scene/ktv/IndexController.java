package io.agora.scene.ktv;

import java.util.Date;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

  @GetMapping("/")
  public ModelAndView view() {
    return new ModelAndView("index", "time", new Date());
  }

}
